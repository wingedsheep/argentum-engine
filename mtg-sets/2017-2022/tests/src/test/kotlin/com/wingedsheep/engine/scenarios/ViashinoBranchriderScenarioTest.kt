package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedMountain271
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ViashinoBranchrider
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ViashinoBranchriderScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(listOf(ViashinoBranchrider, DominariaUnitedMountain271, DominariaUnitedForest274))
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (kicked in listOf(false, true)) {
        test("${if (kicked) "kicked" else "unkicked"} entry has the right counters without a trigger and can attack immediately") {
            val d = driver()
            val rider = d.putCardInHand(d.player1, "Viashino Branchrider")
            d.putLandOnBattlefield(d.player1, "Mountain")
            repeat(3) { d.putLandOnBattlefield(d.player1, "Forest") }
            d.submit(CastSpell(
                playerId = d.player1,
                cardId = rider,
                declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                paymentStrategy = PaymentStrategy.AutoPay,
            )).outcome shouldBe Outcome.Done
            d.bothPass()

            d.stackSize shouldBe 0
            (d.state.getEntity(rider)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe if (kicked) 2 else 0
            d.state.projectedState.getPower(rider) shouldBe if (kicked) 3 else 1
            d.state.projectedState.getToughness(rider) shouldBe if (kicked) 3 else 1
            d.passPriorityUntil(Step.DECLARE_ATTACKERS)
            d.declareAttackers(d.player1, listOf(rider), d.player2).outcome shouldBe Outcome.Done
        }
    }

    test("multiple pumps accumulate only until cleanup while kicked counters remain") {
        val d = driver()
        val rider = d.putCardInHand(d.player1, "Viashino Branchrider")
        d.putLandOnBattlefield(d.player1, "Mountain")
        repeat(3) { d.putLandOnBattlefield(d.player1, "Forest") }
        d.submit(CastSpell(
            playerId = d.player1,
            cardId = rider,
            declaredCostSlot = ChoiceSlot.KICKED,
            paymentStrategy = PaymentStrategy.AutoPay,
        )).outcome shouldBe Outcome.Done
        d.bothPass()
        repeat(6) { d.putLandOnBattlefield(d.player1, "Mountain") }
        repeat(2) {
            d.submit(ActivateAbility(
                playerId = d.player1,
                sourceId = rider,
                abilityId = ViashinoBranchrider.activatedAbilities.single().id,
            )).outcome shouldBe Outcome.Done
            d.bothPass()
        }
        d.state.projectedState.getPower(rider) shouldBe 7
        d.state.projectedState.getToughness(rider) shouldBe 3
        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.state.projectedState.getPower(rider) shouldBe 3
        d.state.projectedState.getToughness(rider) shouldBe 3
        d.state.getEntity(rider)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
    }
})
