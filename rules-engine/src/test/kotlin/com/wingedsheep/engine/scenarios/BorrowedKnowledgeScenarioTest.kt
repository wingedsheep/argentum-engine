package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Borrowed Knowledge ({2}{R}{W} sorcery):
 *
 *   Choose one —
 *   • Discard your hand, then draw cards equal to the number of cards in target opponent's hand.
 *   • Discard your hand, then draw cards equal to the number of cards discarded this way.
 *
 * Both modes discard the whole hand first, then draw. Mode 1 reads the opponent's hand size at
 * resolution; mode 2 reads the count of cards just discarded (`discardedHand_count`).
 */
class BorrowedKnowledgeScenarioTest : ScenarioTestBase() {

    init {
        // -------------------------------------------------------------------
        // Mode 1: draw equal to target opponent's hand size
        // -------------------------------------------------------------------
        test("Borrowed Knowledge mode 1 discards your hand and draws equal to opponent's hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Borrowed Knowledge")
                .withCardInHand(1, "Grizzly Bears") // 1 extra card -> discarded
                .withCardInHand(1, "Hill Giant")    // another extra card -> discarded
                // Opponent holds 3 cards in hand.
                .withCardInHand(2, "Mountain")
                .withCardInHand(2, "Plains")
                .withCardInHand(2, "Forest")
                // Caster's library: enough to draw 3.
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val knowledge = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                    ?.name == "Borrowed Knowledge"
            }

            // Cast choosing mode 0 (the first bullet), targeting the opponent (player 2).
            val result = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = knowledge,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    chosenModes = listOf(0),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(game.player2Id))),
                )
            )
            result.error shouldBe null
            game.resolveStack()

            withClue("the 2 non-spell cards in hand were discarded") {
                game.graveyardSize(1) shouldBe 3 // Borrowed Knowledge + Grizzly Bears + Hill Giant
            }
            withClue("drew cards equal to opponent's 3-card hand") {
                game.handSize(1) shouldBe 3
            }
            withClue("opponent's hand is untouched") {
                game.handSize(2) shouldBe 3
            }
        }

        // -------------------------------------------------------------------
        // Mode 2: draw equal to the number of cards discarded this way
        // -------------------------------------------------------------------
        test("Borrowed Knowledge mode 2 draws equal to the number of cards it discarded") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Borrowed Knowledge")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withCardInHand(1, "Lightning Bolt") // 3 extra cards -> discard 3
                // Library has 5 so we can prove we draw exactly 3.
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val knowledge = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                    ?.name == "Borrowed Knowledge"
            }

            val result = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = knowledge,
                    chosenModes = listOf(1),
                )
            )
            result.error shouldBe null
            game.resolveStack()

            withClue("the 3 non-spell cards in hand were discarded") {
                game.graveyardSize(1) shouldBe 4 // Borrowed Knowledge + 3 discarded cards
            }
            withClue("drew exactly the 3 cards discarded this way") {
                game.handSize(1) shouldBe 3
            }
        }
    }
}
