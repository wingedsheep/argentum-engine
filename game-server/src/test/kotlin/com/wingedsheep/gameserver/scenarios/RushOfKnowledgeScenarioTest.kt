package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rush of Knowledge.
 *
 * Card reference:
 * - Rush of Knowledge ({4}{U}): Sorcery
 *   "Draw cards equal to the greatest mana value among permanents you control."
 */
class RushOfKnowledgeScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.withLibraryCards(playerNumber: Int, cardName: String, count: Int): ScenarioBuilder {
        repeat(count) { withCardInLibrary(playerNumber, cardName) }
        return this
    }

    init {
        context("Rush of Knowledge") {

            test("draws cards equal to the greatest mana value among permanents you control") {
                // Siege-Gang Commander has MV 5, so should draw 5 cards
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rush of Knowledge")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Siege-Gang Commander") // MV 5
                    .withLibraryCards(1, "Island", 10)
                    .withLibraryCards(2, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                val castResult = game.castSpell(1, "Rush of Knowledge")
                withClue("Casting Rush of Knowledge should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Cast from hand (-1) and drew 5 (+5), net +4
                withClue("Player should have drawn 5 cards (greatest MV = Siege-Gang Commander at 5)") {
                    game.handSize(1) shouldBe initialHandSize - 1 + 5
                }

                withClue("Rush of Knowledge should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Rush of Knowledge") shouldBe true
                }
            }

            test("draws zero cards when only lands are controlled") {
                // Lands have MV 0, so draws 0
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rush of Knowledge")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withLibraryCards(1, "Island", 5)
                    .withLibraryCards(2, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                val castResult = game.castSpell(1, "Rush of Knowledge")
                withClue("Casting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Player should draw 0 cards (only lands with MV 0)") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }

            test("uses the greatest mana value among multiple permanents") {
                // Glory Seeker (MV 2) and Goblin Warchief (MV 3) — should draw 3
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Rush of Knowledge")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Glory Seeker")    // MV 2
                    .withCardOnBattlefield(1, "Goblin Warchief") // MV 3
                    .withLibraryCards(1, "Island", 10)
                    .withLibraryCards(2, "Island", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                val castResult = game.castSpell(1, "Rush of Knowledge")
                withClue("Casting should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Player should draw 3 cards (greatest MV = Centaur Courser at 3)") {
                    game.handSize(1) shouldBe initialHandSize - 1 + 3
                }
            }
        }
    }
}
