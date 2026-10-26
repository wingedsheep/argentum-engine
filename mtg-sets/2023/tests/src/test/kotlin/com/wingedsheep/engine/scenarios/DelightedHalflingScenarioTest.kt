package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.DelightedHalfling
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Delighted Halfling — "{T}: Add one mana of any color. Spend this mana only to cast a legendary
 * spell, and that spell can't be countered." Exercises the any-color ability composing
 * `ManaRestriction.LegendarySpellsOnly()` with the `MakesSpellUncounterable` rider: the mana pays for
 * a legendary spell (but not a non-legendary one) and the legendary spell it pays for can't be countered.
 */
class DelightedHalflingScenarioTest : FunSpec({

    // The any-color, legendary-only ability is the second ability ([0] is "{T}: Add {C}").
    val anyColorAbilityId = DelightedHalfling.activatedAbilities[1].id

    val LegendaryDummy = card("Halfling Test Legend") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Avatar"
        power = 1
        toughness = 1
    }
    val PlainDummy = card("Halfling Test Commoner") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(LegendaryDummy, PlainDummy))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    // Activate the any-color ability, choosing green, leaving one legendary-only green mana in the pool.
    fun GameTestDriver.tapForLegendaryMana(you: EntityId): EntityId {
        val halfling = putCreatureOnBattlefield(you, "Delighted Halfling")
        removeSummoningSickness(halfling)
        val result = submit(ActivateAbility(playerId = you, sourceId = halfling, abilityId = anyColorAbilityId))
        (result.outcome is Outcome.Paused) shouldBe true
        val decision = pendingDecision as ChooseColorDecision
        submitDecision(you, ColorChosenResponse(decision.id, Color.GREEN))
        return halfling
    }

    test("produces one legendary-spells-only mana that pays for a legendary spell") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.tapForLegendaryMana(you)

        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 1
        pool.restrictedMana.single().restriction shouldBe ManaRestriction.LegendarySpellsOnly()

        val legend = d.putCardInHand(you, "Halfling Test Legend")
        val cast = d.submit(CastSpell(playerId = you, cardId = legend, paymentStrategy = PaymentStrategy.FromPool))
        cast.error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Halfling Test Legend") shouldNotBe null
    }

    test("the legendary-only mana cannot pay for a non-legendary spell") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.tapForLegendaryMana(you)

        val plain = d.putCardInHand(you, "Halfling Test Commoner")
        val cast = d.submit(CastSpell(playerId = you, cardId = plain, paymentStrategy = PaymentStrategy.FromPool))
        cast.error shouldNotBe null // restricted mana can't pay a non-legendary spell
    }

    test("a legendary spell paid with this mana can't be countered") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.tapForLegendaryMana(you)

        val legend = d.putCardInHand(you, "Halfling Test Legend")
        val counterspell = d.putCardInHand(opponent, "Counterspell")

        // Cast the legendary spell paying from the (uncounterable-rider) restricted pool.
        d.submit(CastSpell(playerId = you, cardId = legend, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.stackSize shouldBe 1
        d.passPriority(you)

        // Opponent tries to counter it.
        d.giveMana(opponent, Color.BLUE, 2)
        val top = d.getTopOfStack()!!
        d.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(top)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        d.stackSize shouldBe 2 // Counterspell really is on the stack targeting the legendary spell

        d.bothPass() // Counterspell resolves but the legendary spell can't be countered
        d.bothPass() // the legendary spell resolves

        d.findPermanent(you, "Halfling Test Legend") shouldNotBe null
    }
})
