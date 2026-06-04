package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.TersaLightshatter
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Tersa Lightshatter (TDM, {2}{R}, 3/3, Haste).
 *
 * - ETB: discard up to two cards, then draw that many cards (loot; draw count = number discarded).
 * - Attacks (intervening "if" 7+ cards in graveyard): exile a random graveyard card, may play it
 *   this turn.
 */
class TersaLightshatterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TersaLightshatter))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1MainPhase() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    // Resolve the stack until a player input is required (the spell resolves, then its ETB trigger).
    fun GameTestDriver.passUntilPendingDecision() {
        var safety = 0
        while (pendingDecision == null && state.stack.isNotEmpty() && safety < 20) {
            bothPass()
            safety++
        }
    }

    test("ETB loot: discarding two cards draws two cards") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.advanceToPlayer1MainPhase()

        // Put Tersa plus two spare cards in hand; give plenty of lands for the cast.
        repeat(3) { driver.putLandOnBattlefield(driver.player1, "Mountain") }
        val tersa = driver.putCardInHand(driver.player1, "Tersa Lightshatter")
        val discardA = driver.putCardInHand(driver.player1, "Mountain")
        val discardB = driver.putCardInHand(driver.player1, "Mountain")

        val graveyardBefore = driver.getGraveyard(driver.player1).size

        driver.castSpell(driver.player1, tersa)
        // Resolve the spell, then its ETB trigger, which prompts for the discard selection.
        driver.passUntilPendingDecision()

        val select = driver.pendingDecision as SelectCardsDecision
        select.playerId shouldBe driver.player1
        driver.submitCardSelection(driver.player1, listOf(discardA, discardB))

        // Two cards discarded → two drawn. Graveyard grew by exactly the two discards.
        driver.getGraveyard(driver.player1).size shouldBe graveyardBefore + 2
    }

    test("ETB loot: declining to discard draws zero (graveyard unchanged)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.advanceToPlayer1MainPhase()

        repeat(3) { driver.putLandOnBattlefield(driver.player1, "Mountain") }
        val tersa = driver.putCardInHand(driver.player1, "Tersa Lightshatter")

        val graveyardBefore = driver.getGraveyard(driver.player1).size

        driver.castSpell(driver.player1, tersa)
        driver.passUntilPendingDecision()

        val select = driver.pendingDecision as SelectCardsDecision
        select.playerId shouldBe driver.player1
        driver.submitCardSelection(driver.player1, emptyList())

        driver.getGraveyard(driver.player1).size shouldBe graveyardBefore
    }

    test("attack with 7+ cards in graveyard exiles a random card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val tersa = driver.putCreatureOnBattlefield(driver.player1, "Tersa Lightshatter")
        driver.removeSummoningSickness(tersa)
        repeat(7) { driver.putCardInGraveyard(driver.player1, "Mountain") }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (driver.activePlayer != driver.player1 && safety < 50) {
            driver.bothPass()
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }

        val graveyardBefore = driver.getGraveyard(driver.player1).size
        val exileBefore = driver.getExile(driver.player1).size

        driver.declareAttackers(driver.player1, mapOf(tersa to driver.player2))
        // Attack trigger goes on the stack; resolve it (random selection needs no input).
        driver.bothPass()

        driver.getExile(driver.player1).size shouldBe exileBefore + 1
        driver.getGraveyard(driver.player1).size shouldBe graveyardBefore - 1
    }

    test("attack with fewer than 7 cards in graveyard does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val tersa = driver.putCreatureOnBattlefield(driver.player1, "Tersa Lightshatter")
        driver.removeSummoningSickness(tersa)
        repeat(6) { driver.putCardInGraveyard(driver.player1, "Mountain") }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (driver.activePlayer != driver.player1 && safety < 50) {
            driver.bothPass()
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }

        val graveyardBefore = driver.getGraveyard(driver.player1).size
        val exileBefore = driver.getExile(driver.player1).size

        driver.declareAttackers(driver.player1, mapOf(tersa to driver.player2))
        driver.bothPass()

        driver.getExile(driver.player1).size shouldBe exileBefore
        driver.getGraveyard(driver.player1).size shouldBe graveyardBefore
    }
})
