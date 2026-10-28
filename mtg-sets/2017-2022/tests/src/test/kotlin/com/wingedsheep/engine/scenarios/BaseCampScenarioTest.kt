package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.cards.BaseCamp
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Base Camp (ZNR #257) — the any-color ability's mana is spendable only on a Cleric, Rogue,
 * Warrior, or Wizard spell, or on an ability of a Cleric, Rogue, Warrior, or Wizard permanent.
 * The tests pin both sides of the "or" and the refusal of everything else.
 */
class BaseCampScenarioTest : FunSpec({

    val wizardSpell = card("Test Wizard Spell") {
        manaCost = "{1}"
        typeLine = "Creature — Human Wizard"
        power = 1
        toughness = 1
    }

    val goblinSpell = card("Test Goblin Spell") {
        manaCost = "{1}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }

    val rogueWithAbility = card("Test Rogue Pinger") {
        manaCost = "{3}"
        typeLine = "Creature — Human Rogue"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    val beastWithAbility = card("Test Beast Pinger") {
        manaCost = "{3}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BaseCamp, wizardSpell, goblinSpell, rogueWithAbility, beastWithAbility))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.tapForPartyMana(playerId: EntityId) {
        val camp = putLandOnBattlefield(playerId, "Base Camp")
        untapPermanent(camp)
        submit(
            ActivateAbility(playerId, camp, BaseCamp.activatedAbilities[1].id, manaColorChoice = Color.RED)
        ).outcome shouldBe Outcome.Done
        withClue("Base Camp's second ability makes one restricted mana") {
            state.getEntity(playerId)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 1
        }
    }

    test("enters tapped and taps for {C}") {
        val d = driver()
        val p1 = d.player1
        val camp = d.putLandOnBattlefield(p1, "Base Camp")
        d.untapPermanent(camp)
        d.submit(ActivateAbility(p1, camp, BaseCamp.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        d.state.getEntity(p1)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
    }

    test("the restricted mana pays for a Wizard spell") {
        val d = driver()
        val p1 = d.player1
        d.tapForPartyMana(p1)
        val wizard = d.putCardInHand(p1, "Test Wizard Spell")
        d.submit(CastSpell(playerId = p1, cardId = wizard, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldBe Outcome.Done
    }

    test("the restricted mana cannot pay for a non-party spell") {
        val d = driver()
        val p1 = d.player1
        d.tapForPartyMana(p1)
        val goblin = d.putCardInHand(p1, "Test Goblin Spell")
        d.submit(CastSpell(playerId = p1, cardId = goblin, paymentStrategy = PaymentStrategy.FromPool))
            .outcome shouldNotBe Outcome.Done
    }

    test("the restricted mana pays for an ability of a Rogue on the battlefield") {
        val d = driver()
        val p1 = d.player1
        val rogue = d.putCreatureOnBattlefield(p1, "Test Rogue Pinger")
        d.tapForPartyMana(p1)
        d.submit(
            ActivateAbility(
                p1, rogue, rogueWithAbility.activatedAbilities[0].id,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
    }

    test("the restricted mana cannot pay for an ability of a non-party creature") {
        val d = driver()
        val p1 = d.player1
        val beast = d.putCreatureOnBattlefield(p1, "Test Beast Pinger")
        d.tapForPartyMana(p1)
        d.submit(
            ActivateAbility(
                p1, beast, beastWithAbility.activatedAbilities[0].id,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldNotBe Outcome.Done
    }
})
