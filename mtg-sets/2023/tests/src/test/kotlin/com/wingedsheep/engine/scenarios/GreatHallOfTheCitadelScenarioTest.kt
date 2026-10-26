package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GreatHallOfTheCitadel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Great Hall of the Citadel — "{1}, {T}: Add two mana in any combination of colors. Spend this mana
 * only to cast legendary spells." Exercises the any-combination ability composing
 * `ManaRestriction.LegendarySpellsOnly()`: two legendary-only mana that pay for a legendary spell but
 * not a non-legendary one.
 */
class GreatHallOfTheCitadelScenarioTest : FunSpec({

    // The any-combination, legendary-only ability is the second ability ([0] is "{T}: Add {C}").
    val anyCombinationAbilityId = GreatHallOfTheCitadel.activatedAbilities[1].id

    val LegendaryDummy = card("Citadel Test Legend") {
        manaCost = "{2}"
        typeLine = "Legendary Creature — Avatar"
        power = 2
        toughness = 2
    }
    val PlainDummy = card("Citadel Test Commoner") {
        manaCost = "{2}"
        typeLine = "Creature — Soldier"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(LegendaryDummy, PlainDummy))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
    }

    // Activate the {1},{T} any-combination ability, choosing green for both pips, leaving two
    // legendary-only green mana in the pool.
    fun GameTestDriver.tapForTwoLegendaryMana(you: EntityId) {
        putLandOnBattlefield(you, "Great Hall of the Citadel")
        val greatHall = findPermanent(you, "Great Hall of the Citadel")!!
        giveMana(you, Color.WHITE, 1) // pays the {1} activation cost
        submit(ActivateAbility(playerId = you, sourceId = greatHall, abilityId = anyCombinationAbilityId))
        // Five allowed colors → the executor prompts pip-by-pip; answer both.
        repeat(2) {
            val decision = pendingDecision as ChooseColorDecision
            submitDecision(you, ColorChosenResponse(decision.id, Color.GREEN))
        }
    }

    test("produces two legendary-spells-only mana that pay for a legendary spell") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.tapForTwoLegendaryMana(you)

        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 2
        pool.restrictedMana.all { it.restriction == ManaRestriction.LegendarySpellsOnly() } shouldBe true

        val legend = d.putCardInHand(you, "Citadel Test Legend")
        val cast = d.submit(CastSpell(playerId = you, cardId = legend, paymentStrategy = PaymentStrategy.FromPool))
        cast.error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Citadel Test Legend") shouldNotBe null
    }

    test("the legendary-only mana cannot pay for a non-legendary spell") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.tapForTwoLegendaryMana(you)

        val plain = d.putCardInHand(you, "Citadel Test Commoner")
        val cast = d.submit(CastSpell(playerId = you, cardId = plain, paymentStrategy = PaymentStrategy.FromPool))
        cast.error shouldNotBe null // restricted mana can't pay a non-legendary spell
    }
})
