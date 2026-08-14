package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.EnteredPermanentRecord
import com.wingedsheep.engine.state.components.player.PermanentsEnteredUnderControlThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.AkalPakal
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Akal Pakal, First Among Equals (LCI).
 *
 * Akal Pakal: {2}{U} Legendary Creature — Human Advisor 1/5
 * "At the beginning of each player's end step, if an artifact entered the battlefield under your
 * control this turn, look at the top two cards of your library. Put one of them into your hand
 * and the other into your graveyard."
 *
 * Key rules (CR 603.4 intervening-if):
 * - The condition "if an artifact entered the battlefield under your control this turn" is checked
 *   both when the trigger would fire and when the ability would resolve.
 * - "Your control" refers to Akal Pakal's controller, not the active player.
 */
class AkalPakalScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(AkalPakal)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    test("trigger fires at end step and controller keeps one of the top two library cards when an artifact entered this turn") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(you, "Akal Pakal, First Among Equals")

        // Simulate an artifact having entered the battlefield under your control this turn
        // by injecting the turn-tracker component directly (mirrors how the engine records ETBs).
        d.replaceState(
            d.state.updateEntity(you) { container ->
                container.with(
                    PermanentsEnteredUnderControlThisTurnComponent(
                        listOf(EnteredPermanentRecord(you, setOf(CardType.ARTIFACT)))
                    )
                )
            }
        )

        // Place two known cards on top of the library (second call = top of library).
        val second = d.putCardOnTopOfLibrary(you, "Forest")
        val first = d.putCardOnTopOfLibrary(you, "Forest")

        // Advance to the end step — the intervening-if condition passes, so the trigger fires
        // and goes on the stack (Rule 603.4 trigger-time check). passPriorityUntil stops with the
        // ability still on the stack, so resolve it to reach the look-at-top-two decision.
        d.passPriorityUntil(Step.END)
        d.state.stack.size shouldBe 1
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The lookAtTopAndKeep effect pauses for the player to choose which card to keep.
        d.isPaused shouldBe true
        val decision = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options.size shouldBe 2
        decision.options shouldContain first
        decision.options shouldContain second

        // Keep the first (top) card; the second goes to the graveyard.
        d.submitDecision(you, CardsSelectedResponse(decisionId = decision.id, selectedCards = listOf(first)))

        d.getHand(you) shouldContain first
        d.getHand(you) shouldNotContain second
        d.getGraveyard(you) shouldContain second
        d.getGraveyard(you) shouldNotContain first
    }

    test("trigger does NOT fire when no artifact entered the battlefield under your control this turn") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(you, "Akal Pakal, First Among Equals")

        // No permanent-entry log set — the intervening-if condition
        // fails at trigger time, so the ability must not be placed on the stack (Rule 603.4).
        d.passPriorityUntil(Step.END)

        d.state.stack.size shouldBe 0
    }
})
