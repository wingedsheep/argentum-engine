package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Torment of Venom — {2}{B}{B} instant: "Put three -1/-1 counters on target creature. Its
 * controller loses 3 life unless they sacrifice another nonland permanent of their choice or
 * discard a card."
 *
 * "Another" excludes the target creature itself, which is still on the battlefield while its
 * controller is tormented (state-based actions wait for the spell to finish resolving).
 */
class TormentOfVenomScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20, skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.torment(target: EntityId) {
        val spell = putCardInHand(player1, "Torment of Venom")
        giveMana(player1, Color.BLACK, 4)
        submit(
            CastSpell(
                playerId = player1,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(target)),
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).error shouldBe null
        bothPass()
    }

    test("sacrificing another nonland permanent spares the life, and the target can't be the sacrifice") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.torment(giant)

        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.playerId shouldBe d.player2
        choice.options.size shouldBe 3
        d.submitDecision(d.player2, OptionChosenResponse(choice.id, 0))

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options shouldContainExactly listOf(bears)
        d.submitCardSelection(d.player2, listOf(bears))

        d.getLifeTotal(d.player2) shouldBe 20
        (bears in d.state.getBattlefield()) shouldBe false
        // Three -1/-1 counters on a 3/3: it dies once the spell has resolved.
        (giant in d.state.getBattlefield()) shouldBe false
    }

    test("with only the target to sacrifice, the sacrifice option isn't offered") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")

        d.torment(giant)

        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        // Discard a card, or lose 3 life — no sacrifice: the giant itself isn't "another".
        choice.options.size shouldBe 2
        d.submitDecision(d.player2, OptionChosenResponse(choice.id, choice.options.lastIndex))

        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player2) shouldBe 17
        (giant in d.state.getBattlefield()) shouldBe false
    }

    test("discarding a card spares the life") {
        val d = driver()
        val giant = d.putCreatureOnBattlefield(d.player2, "Hill Giant")
        val handBefore = d.state.getHand(d.player2).size

        d.torment(giant)

        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(d.player2, OptionChosenResponse(choice.id, 0))
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player2, pick.options.take(1))

        d.getLifeTotal(d.player2) shouldBe 20
        d.state.getHand(d.player2).size shouldBe handBefore - 1
    }
})
