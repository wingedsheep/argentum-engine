package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Lethal Sting — {2}{B} sorcery: "As an additional cost to cast this spell, put a -1/-1 counter on a
 * creature you control. Destroy target creature."
 */
class LethalStingScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20, skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.sting(spell: EntityId, target: EntityId, counterOn: EntityId) = submit(
        CastSpell(
            playerId = player1,
            cardId = spell,
            targets = listOf(ChosenTarget.Permanent(target)),
            paymentStrategy = PaymentStrategy.AutoPay,
            additionalCostPayment = AdditionalCostPayment(counterPlacementTargets = listOf(counterOn)),
        )
    )

    fun GameTestDriver.minusCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0

    test("a -1/-1 counter on your own creature pays for destroying the opponent's creature") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val sting = d.putCardInHand(d.player1, "Lethal Sting")
        d.giveMana(d.player1, Color.BLACK, 3)

        val offer = d.legalActions(d.player1)
            .single { (it.action as? CastSpell)?.cardId == sting && it.actionType == "CastSpell" }
            .additionalCostInfo!!
        offer.validCounterPlacementTargets shouldContainExactly listOf(bear)

        d.sting(sting, giant, bear).error shouldBe null
        d.minusCounters(bear) shouldBe 1
        d.bothPass()

        (giant in d.state.getBattlefield()) shouldBe false
        (bear in d.state.getBattlefield()) shouldBe true
        d.state.projectedState.getPower(bear) shouldBe 1
    }

    test("it can't be cast without a creature of your own to take the counter") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val sting = d.putCardInHand(d.player1, "Lethal Sting")
        d.giveMana(d.player1, Color.BLACK, 3)
        d.legalActions(d.player1).none { (it.action as? CastSpell)?.cardId == sting } shouldBe true
    }

    test("a 1/1 paying the cost dies to the counter, and the spell still resolves") {
        val d = driver()
        val elf = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val sting = d.putCardInHand(d.player1, "Lethal Sting")
        d.giveMana(d.player1, Color.BLACK, 3)

        d.sting(sting, giant, elf).error shouldBe null
        d.bothPass()

        (elf in d.state.getBattlefield()) shouldBe false
        (giant in d.state.getBattlefield()) shouldBe false
    }
})
