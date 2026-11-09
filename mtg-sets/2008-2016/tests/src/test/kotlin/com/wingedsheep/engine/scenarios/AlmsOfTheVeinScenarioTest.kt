package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Alms of the Vein (SOI #98) — {2}{B} Sorcery.
 *
 *   Target opponent loses 3 life and you gain 3 life.
 *   Madness {B}
 */
class AlmsOfTheVeinScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, driver.getOpponent(player))
    }

    test("target opponent loses 3 life and you gain 3 life") {
        val (driver, player, opponent) = newGame()
        val alms = driver.putCardInHand(player, "Alms of the Vein")
        driver.giveMana(player, Color.BLACK, 3)

        driver.castSpell(player, alms, targets = listOf(opponent)).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(player) shouldBe 23
    }

    test("discarded, it can be cast for its madness cost of {B}") {
        val (driver, player, opponent) = newGame()
        val alms = driver.putCardInHand(player, "Alms of the Vein")

        // Discard it as Tormenting Voice's additional cost.
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(alms)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getExile(player) shouldContain alms

        driver.giveMana(player, Color.BLACK, 1)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
        driver.submitYesNo(player, true)
        if (driver.pendingDecision is ChooseTargetsDecision) {
            driver.submitTargetSelection(player, listOf(opponent))
        }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(player) shouldBe 23
        driver.getGraveyard(player) shouldContain alms
    }
})
