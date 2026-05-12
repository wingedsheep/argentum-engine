package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Romantic Rendezvous.
 *
 * Card reference:
 * - Romantic Rendezvous ({1}{R}): Sorcery
 *   "Discard a card, then draw two cards."
 */
class RomanticRendezvousScenarioTest : ScenarioTestBase() {

    init {
        context("Romantic Rendezvous — discard a card, draw two") {

            test("discards one card then draws two after resolution") {
                // Starting hand: [Romantic Rendezvous, Grizzly Bears, Glory Seeker] = 3 cards
                // Expected hand after: 3 - 1 (cast RR) - 1 (discard GB) + 2 (draw) = 3 cards
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Romantic Rendezvous")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Glory Seeker")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialOpponentHandSize = game.handSize(2)
                val initialOpponentLibrarySize = game.librarySize(2)
                val initialOpponentLife = game.getLifeTotal(2)

                // Accumulate events across every action so we can assert ordering below.
                val emittedEvents = mutableListOf<GameEvent>()

                val castResult = game.castSpell(1, "Romantic Rendezvous")
                emittedEvents += castResult.events
                withClue("Casting Romantic Rendezvous should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Spell resolves; engine presents a SelectCardsDecision for the discard
                val resolveResults = game.resolveStack()
                resolveResults.forEach { emittedEvents += it.events }

                withClue("Engine should present a discard selection decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Player 1 discards Grizzly Bears
                val bearInHand = game.findCardsInHand(1, "Grizzly Bears")
                withClue("Grizzly Bears should still be in hand when discard decision is presented") {
                    bearInHand.isNotEmpty() shouldBe true
                }
                val selectResult = game.selectCards(bearInHand)
                emittedEvents += selectResult.events

                // After discard, the engine automatically draws two cards
                withClue("Romantic Rendezvous should be in the caster's graveyard after resolution") {
                    game.isInGraveyard(1, "Romantic Rendezvous") shouldBe true
                }
                withClue("Grizzly Bears should be in the caster's graveyard (discarded)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Hand size should be 3: started 3, cast RR (−1), discard GB (−1), draw two (+2)") {
                    game.handSize(1) shouldBe 3
                }
                withClue("Opponent's hand should be unchanged") {
                    game.handSize(2) shouldBe initialOpponentHandSize
                }
                withClue("Opponent's library should be unchanged") {
                    game.librarySize(2) shouldBe initialOpponentLibrarySize
                }
                withClue("Opponent's life total should be unchanged") {
                    game.getLifeTotal(2) shouldBe initialOpponentLife
                }

                // CR sequential resolution: 'A, then B' means A fully resolves before B.
                // The discard's CardsDiscardedEvent MUST be emitted before CardsDrawnEvent
                // so that 'whenever you discard' triggers, madness, and dredge see the
                // correct intermediate state.
                val discardIdx = emittedEvents.indexOfFirst {
                    it is CardsDiscardedEvent && it.playerId == game.player1Id
                }
                val drawIdx = emittedEvents.indexOfFirst {
                    it is CardsDrawnEvent && it.playerId == game.player1Id && it.count == 2
                }
                withClue("Caster should have emitted a CardsDiscardedEvent") {
                    (discardIdx >= 0) shouldBe true
                }
                withClue("Caster should have emitted a CardsDrawnEvent for 2 cards") {
                    (drawIdx >= 0) shouldBe true
                }
                withClue("Discard must emit before draw so 'whenever you discard' triggers see correct state") {
                    (discardIdx < drawIdx) shouldBe true
                }
            }
        }
    }
}
