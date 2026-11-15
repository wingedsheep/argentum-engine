package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.EnhancedAwareness
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Enhanced Awareness (Fate Reforged #33) — {4}{U} Instant.
 *
 * "Draw three cards, then discard a card."
 *
 * The discard comes after the draw, so a card just drawn can be the one discarded.
 */
class EnhancedAwarenessScenarioTest : FunSpec({

    test("draws three, then discards one of the cards in hand, which may be a drawn card") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + EnhancedAwareness)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val awareness = driver.putCardInHand(me, "Enhanced Awareness")
        val bolt = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val handBefore = driver.getHandSize(me)
        driver.giveMana(me, Color.BLUE, 5)
        driver.castSpell(me, awareness).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.pendingDecision !is SelectCardsDecision && guard++ < 10) driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain bolt
        driver.submitCardSelection(me, listOf(bolt)).error shouldBe null

        driver.getHandSize(me) shouldBe handBefore - 1 + 3 - 1
        driver.getHand(me) shouldNotContain bolt
        driver.getGraveyard(me) shouldContainExactlyInAnyOrder listOf(awareness, bolt)
    }
})
