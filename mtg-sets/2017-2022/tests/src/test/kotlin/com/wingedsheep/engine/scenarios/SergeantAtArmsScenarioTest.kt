package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sergeant-at-Arms (DOM) — {2}{W} 2/3 Human Soldier with Kicker {2}{W} and "When this creature
 * enters, if it was kicked, create two 1/1 white Soldier creature tokens."
 *
 * Proves the intervening "if" (CR 603.4): unkicked, the ETB never triggers — nothing goes on the
 * stack — rather than triggering and doing nothing on resolution.
 */
class SergeantAtArmsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 30))
        return d
    }

    fun GameTestDriver.soldierTokens() =
        getCreatures(player1).count { getCardName(it) != "Sergeant-at-Arms" }

    test("kicked: the ETB creates two Soldier tokens") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sergeant = d.putCardInHand(you, "Sergeant-at-Arms")
        repeat(6) { d.putLandOnBattlefield(you, "Plains") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = sergeant,
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature; the kicked ETB trigger goes on the stack
        d.stackSize shouldBe 1
        d.bothPass()

        d.soldierTokens() shouldBe 2
    }

    test("not kicked: the ETB never goes on the stack") {
        val d = driver()
        val you = d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val sergeant = d.putCardInHand(you, "Sergeant-at-Arms")
        repeat(3) { d.putLandOnBattlefield(you, "Plains") }

        d.submit(
            CastSpell(
                playerId = you,
                cardId = sergeant,
                declaredCostSlot = null,
                paymentStrategy = PaymentStrategy.AutoPay,
            ),
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.stackSize shouldBe 0
        d.soldierTokens() shouldBe 0
    }
})
