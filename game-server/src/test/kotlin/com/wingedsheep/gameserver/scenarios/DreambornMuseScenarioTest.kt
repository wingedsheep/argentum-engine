package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dreamborn Muse.
 *
 * Dreamborn Muse: {2}{U}{U}
 * Creature — Spirit 2/2
 * At the beginning of each player's upkeep, that player mills X cards,
 * where X is the number of cards in their hand.
 */
class DreambornMuseScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.ScenarioBuilder.withCardsInLibrary(
        playerNumber: Int, cardName: String, count: Int
    ): ScenarioBuilder {
        repeat(count) { withCardInLibrary(playerNumber, cardName) }
        return this
    }

    private fun ScenarioTestBase.TestGame.advanceToUpkeepTrigger() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        // Resolve the trigger on the stack
        var iterations = 0
        while (state.stack.isNotEmpty() && iterations < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
            iterations++
        }
    }

    init {
        context("Dreamborn Muse upkeep trigger") {

            test("active player mills cards equal to their hand size on their upkeep") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dreamborn Muse")
                    .withCardInHand(1, "Island")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Mountain")
                    .withCardsInLibrary(1, "Plains", 10)
                    .withCardsInLibrary(2, "Plains", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val initialLibrarySize = game.librarySize(1)

                withClue("Player 1 should have 3 cards in hand") {
                    game.handSize(1) shouldBe 3
                }

                game.advanceToUpkeepTrigger()

                withClue("Player 1 should have milled 3 cards (hand size was 3)") {
                    game.librarySize(1) shouldBe initialLibrarySize - 3
                }
                withClue("Player 1 graveyard should have 3 milled cards") {
                    game.graveyardSize(1) shouldBe 3
                }
            }

            test("mills zero cards when hand is empty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dreamborn Muse")
                    .withCardsInLibrary(1, "Plains", 10)
                    .withCardsInLibrary(2, "Plains", 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val initialLibrarySize = game.librarySize(1)

                withClue("Player 1 should have 0 cards in hand") {
                    game.handSize(1) shouldBe 0
                }

                game.advanceToUpkeepTrigger()

                withClue("Library size should be unchanged (milled 0)") {
                    game.librarySize(1) shouldBe initialLibrarySize
                }
            }

            test("triggers on opponent's upkeep too") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Dreamborn Muse")
                    .withCardInHand(2, "Island")
                    .withCardInHand(2, "Forest")
                    .withCardsInLibrary(1, "Plains", 10)
                    .withCardsInLibrary(2, "Plains", 10)
                    .withActivePlayer(2)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                val initialLibrarySize = game.librarySize(2)

                withClue("Player 2 should have 2 cards in hand") {
                    game.handSize(2) shouldBe 2
                }

                game.advanceToUpkeepTrigger()

                withClue("Player 2 should have milled 2 cards (hand size was 2)") {
                    game.librarySize(2) shouldBe initialLibrarySize - 2
                }
                withClue("Player 2 graveyard should have 2 milled cards") {
                    game.graveyardSize(2) shouldBe 2
                }
            }
        }
    }
}
