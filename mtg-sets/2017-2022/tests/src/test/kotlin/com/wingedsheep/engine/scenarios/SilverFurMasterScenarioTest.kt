package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.SilverFurMaster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.dsl.ninjutsuAbility
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SilverFurMasterScenarioTest : FunSpec({

    val ninja = card("Test Ninja") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Human Ninja"
        power = 3
        toughness = 3
        ninjutsu("{1}{B}")
    }

    val pinger = card("Test Pinger") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GainLife(1)
        }
    }

    val rogue = card("Test Rogue") {
        manaCost = "{1}"
        typeLine = "Creature — Human Rogue"
        power = 1
        toughness = 1
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SilverFurMaster, ninja, rogue, pinger))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
    }

    fun GameTestDriver.unblockedLion(): EntityId {
        val lion = putCreatureOnBattlefield(player1, "Savannah Lions").also(::removeSummoningSickness)
        val opponent = getOpponent(player1)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(player1, mapOf(lion to opponent)).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.DECLARE_BLOCKERS)
        declareBlockers(opponent, emptyMap()).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.priorityPlayerId != player1 && state.step == Step.DECLARE_BLOCKERS && guard++ < 4) {
            passPriority(state.priorityPlayerId!!)
        }
        return lion
    }

    test("ninjutsu abilities you activate cost {1} less: a {1}{B} ninjutsu is paid with {B}") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Silver-Fur Master")
        val lion = d.unblockedLion()
        val testNinja = d.putCardInHand(d.player1, "Test Ninja")
        d.giveMana(d.player1, Color.BLACK, 1)

        d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = testNinja,
                abilityId = ninjutsuAbility("{1}{B}").id,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(lion)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).outcome shouldBe Outcome.Done
        while (d.state.stack.isNotEmpty()) d.bothPass()
        d.findPermanent(d.player1, "Test Ninja").shouldNotBeNull()
    }

    test("without the Master the same ninjutsu can't be paid with {B} alone") {
        val d = driver()
        val lion = d.unblockedLion()
        val testNinja = d.putCardInHand(d.player1, "Test Ninja")
        d.giveMana(d.player1, Color.BLACK, 1)
        d.submitExpectFailure(
            ActivateAbility(
                playerId = d.player1,
                sourceId = testNinja,
                abilityId = ninjutsuAbility("{1}{B}").id,
                costPayment = AdditionalCostPayment(bouncedPermanents = listOf(lion)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("only ninjutsu abilities are cheaper: a {1} ability still needs its mana") {
        val d = driver()
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Silver-Fur Master")
        val pinger = d.putPermanentOnBattlefield(d.player1, "Test Pinger")
        val abilityId = d.cardRegistry.getCard("Test Pinger")!!.script.activatedAbilities.single().id
        d.submitExpectFailure(
            ActivateAbility(
                playerId = d.player1,
                sourceId = pinger,
                abilityId = abilityId,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("other Ninja and Rogue creatures you control get +1/+1, the Master itself doesn't") {
        val d = driver()
        val master = d.putCreatureOnBattlefield(d.player1, "Silver-Fur Master")
        val testNinja = d.putCreatureOnBattlefield(d.player1, "Test Ninja")
        val testRogue = d.putCreatureOnBattlefield(d.player1, "Test Rogue")
        val lion = d.putCreatureOnBattlefield(d.player1, "Savannah Lions")
        val projected = d.state.projectedState
        projected.getPower(master) shouldBe 2
        projected.getPower(testNinja) shouldBe 4
        projected.getPower(testRogue) shouldBe 2
        projected.getPower(lion) shouldBe 1
    }
})
