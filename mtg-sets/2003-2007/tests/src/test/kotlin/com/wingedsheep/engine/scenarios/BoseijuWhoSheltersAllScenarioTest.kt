package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.BoseijuWhoSheltersAll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Boseiju, Who Shelters All — "{T}, Pay 2 life: Add {C}. If that mana is spent on an instant or
 * sorcery spell, that spell can't be countered."
 *
 * Exercises the spell-filtered `MakesSpellUncounterable` rider on colorless mana: the {C} pays for
 * anything, but only an instant or sorcery it pays for becomes uncounterable — through the floating
 * pool and through auto-pay alike.
 */
class BoseijuWhoSheltersAllScenarioTest : FunSpec({

    val manaAbilityId = BoseijuWhoSheltersAll.activatedAbilities.single().id

    val TestInstant = card("Boseiju Test Insight") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    val TestCreature = card("Boseiju Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BoseijuWhoSheltersAll, TestInstant, TestCreature))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
    }

    fun GameTestDriver.boseiju(you: EntityId): EntityId =
        putPermanentOnBattlefield(you, "Boseiju, Who Shelters All")

    /** The opponent casts Counterspell at the top of the stack; both players then pass it through. */
    fun GameTestDriver.opponentCounters(you: EntityId, opponent: EntityId) {
        passPriority(you)
        giveMana(opponent, Color.BLUE, 2)
        val counterspell = putCardInHand(opponent, "Counterspell")
        submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(getTopOfStack()!!)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        stackSize shouldBe 2
        bothPass() // Counterspell resolves
    }

    test("enters tapped") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val land = d.putCardInHand(you, "Boseiju, Who Shelters All")
        d.playLand(you, land).error shouldBe null
        d.isTapped(d.findPermanent(you, "Boseiju, Who Shelters All")!!) shouldBe true
    }

    test("tapping pays 2 life and adds one colorless mana") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val boseiju = d.boseiju(you)

        d.submit(ActivateAbility(playerId = you, sourceId = boseiju, abilityId = manaAbilityId)).error shouldBe null

        d.getLifeTotal(you) shouldBe 18
        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.restrictedMana.size shouldBe 1
        pool.restrictedMana.single().color shouldBe null
    }

    test("an instant paid with its mana can't be countered") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val boseiju = d.boseiju(you)
        d.submit(ActivateAbility(playerId = you, sourceId = boseiju, abilityId = manaAbilityId)).error shouldBe null

        val instant = d.putCardInHand(you, "Boseiju Test Insight")
        d.submit(CastSpell(playerId = you, cardId = instant, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.state.getEntity(instant)!!.has<CantBeCounteredComponent>() shouldBe true
        val handBefore = d.state.getZone(you, Zone.HAND).size

        d.opponentCounters(you, opponent)
        d.stackSize shouldBe 1 // the counter did nothing; the instant is still there
        d.bothPass()

        d.state.getZone(you, Zone.HAND).size shouldBe handBefore + 1
    }

    test("a creature spell paid with its mana can still be countered") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val boseiju = d.boseiju(you)
        d.submit(ActivateAbility(playerId = you, sourceId = boseiju, abilityId = manaAbilityId)).error shouldBe null

        // The {C} is unrestricted — it pays for the creature — but the rider does not apply.
        val bear = d.putCardInHand(you, "Boseiju Test Bear")
        d.submit(CastSpell(playerId = you, cardId = bear, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.state.getEntity(bear)!!.has<CantBeCounteredComponent>() shouldBe false

        d.opponentCounters(you, opponent)

        d.stackSize shouldBe 0
        d.findPermanent(you, "Boseiju Test Bear") shouldBe null
        d.state.getZone(you, Zone.GRAVEYARD).contains(bear) shouldBe true
    }

    test("auto-pay tapping Boseiju also makes the instant uncounterable") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.boseiju(you)

        val instant = d.putCardInHand(you, "Boseiju Test Insight")
        d.submit(CastSpell(playerId = you, cardId = instant, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        d.getLifeTotal(you) shouldBe 18
        d.state.getEntity(instant)!!.has<CantBeCounteredComponent>() shouldBe true
    }

    test("auto-pay tapping Boseiju for a creature spell leaves it counterable") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.boseiju(you)

        val bear = d.putCardInHand(you, "Boseiju Test Bear")
        d.submit(CastSpell(playerId = you, cardId = bear, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null

        d.getLifeTotal(you) shouldBe 18
        d.state.getEntity(bear)!!.has<CantBeCounteredComponent>() shouldBe false
        d.state.getEntity(bear) shouldNotBe null
    }
})
