package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class StrengthOfTheCoalitionScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        repeat(3) { putLandOnBattlefield(player1, "Forest") }
        putLandOnBattlefield(player1, "Plains")
    }

    fun GameTestDriver.counters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    for (kicked in listOf(false, true)) {
        test("${if (kicked) "kicked" else "unkicked"} Strength pumps the target with permanent team counters only when kicked") {
            val d = driver()
            val target = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
            val ally = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
            val enemy = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
            val spell = d.putCardInHand(d.player1, "Strength of the Coalition")
            d.submit(CastSpell(
                playerId = d.player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null,
                paymentStrategy = PaymentStrategy.AutoPay,
            )).error shouldBe null
            d.bothPass()
            d.stackSize shouldBe 0
            d.counters(target) shouldBe if (kicked) 1 else 0
            d.counters(ally) shouldBe if (kicked) 1 else 0
            d.counters(enemy) shouldBe 0
            d.state.projectedState.getPower(target) shouldBe if (kicked) 5 else 4
            d.state.projectedState.getToughness(target) shouldBe if (kicked) 5 else 4
            d.state.projectedState.getPower(ally) shouldBe if (kicked) 4 else 3
            d.state.projectedState.getPower(enemy) shouldBe 2

            d.passPriorityUntil(Step.UPKEEP, d.player2)
            d.state.projectedState.getPower(target) shouldBe if (kicked) 3 else 2
            d.state.projectedState.getToughness(target) shouldBe if (kicked) 3 else 2
            d.counters(target) shouldBe if (kicked) 1 else 0
            d.counters(ally) shouldBe if (kicked) 1 else 0
        }
    }

    test("opposing creature cannot be the target") {
        val d = driver()
        val enemy = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, "Strength of the Coalition")
        d.submit(CastSpell(
            playerId = d.player1,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(enemy)),
            paymentStrategy = PaymentStrategy.AutoPay,
        )).error shouldNotBe null
    }

    test("losing the only target prevents kicked team counters") {
        val d = driver()
        val target = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val ally = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        val spell = d.putCardInHand(d.player1, "Strength of the Coalition")
        d.putLandOnBattlefield(d.player2, "Island")
        val bounce = d.putCardInHand(d.player2, "Unsummon")
        d.submit(CastSpell(
            playerId = d.player1,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(target)),
            declaredCostSlot = ChoiceSlot.KICKED,
            paymentStrategy = PaymentStrategy.AutoPay,
        )).error shouldBe null
        d.passPriority(d.player1)
        d.submit(CastSpell(
            playerId = d.player2,
            cardId = bounce,
            targets = listOf(ChosenTarget.Permanent(target)),
            paymentStrategy = PaymentStrategy.AutoPay,
        )).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.stackSize shouldBe 0
        d.counters(ally) shouldBe 0
        d.state.projectedState.getPower(ally) shouldBe 3
    }
})
