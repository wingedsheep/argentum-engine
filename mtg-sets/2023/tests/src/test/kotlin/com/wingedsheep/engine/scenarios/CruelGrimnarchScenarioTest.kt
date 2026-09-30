package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Cruel Grimnarch — "When this creature enters, each opponent discards a card. For each opponent
 * who can't, you gain 4 life."
 *
 * The load-bearing case: "who can't" is decided before the discard. An opponent who discards
 * their last card could discard, so it grants no life.
 */
class CruelGrimnarchScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.setHandSize(playerId: EntityId, keep: Int) {
        val handZone = ZoneKey(playerId, Zone.HAND)
        var s = state
        getHand(playerId).drop(keep).forEach { s = s.removeFromZone(handZone, it) }
        replaceState(s)
    }

    fun GameTestDriver.castGrimnarchAndResolve() {
        val grimnarch = putCardInHand(player1, "Cruel Grimnarch")
        giveMana(player1, Color.BLACK, 6)
        castSpell(player1, grimnarch)
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard++ < 20) {
            if (pendingDecision != null) {
                val choice = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                submitCardSelection(choice.playerId, choice.options.take(choice.minSelections.coerceAtLeast(1)))
            } else {
                passPriority(state.priorityPlayerId!!)
            }
        }
    }

    test("an opponent with cards in hand discards one and you gain no life") {
        val driver = newGame()
        val opponentHand = driver.getHandSize(driver.player2)
        val life = driver.getLifeTotal(driver.player1)

        driver.castGrimnarchAndResolve()

        driver.getHandSize(driver.player2) shouldBe opponentHand - 1
        driver.getLifeTotal(driver.player1) shouldBe life
    }

    test("an opponent with an empty hand can't discard, so you gain 4 life") {
        val driver = newGame()
        driver.setHandSize(driver.player2, 0)
        val life = driver.getLifeTotal(driver.player1)
        val opponentLife = driver.getLifeTotal(driver.player2)

        driver.castGrimnarchAndResolve()

        withClue("the controller gains the life, not the opponent") {
            driver.getLifeTotal(driver.player1) shouldBe life + 4
            driver.getLifeTotal(driver.player2) shouldBe opponentLife
        }
    }

    test("an opponent discarding their last card grants no life") {
        val driver = newGame()
        driver.setHandSize(driver.player2, 1)
        val life = driver.getLifeTotal(driver.player1)

        driver.castGrimnarchAndResolve()

        driver.getHandSize(driver.player2) shouldBe 0
        driver.getGraveyardCardNames(driver.player2).size shouldBe 1
        driver.getLifeTotal(driver.player1) shouldBe life
    }
})
