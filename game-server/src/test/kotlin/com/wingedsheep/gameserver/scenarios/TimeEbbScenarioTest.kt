package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Time Ebb.
 *
 * Card reference:
 * - Time Ebb (2U): Sorcery. "Put target creature on top of its owner's library."
 *
 * Scenario:
 * - Player 1 controls a creature (Grizzly Bears) on the battlefield
 * - Player 2 casts Time Ebb targeting the creature
 * - After resolution, the creature should be on top of Player 1's library
 */
class TimeEbbScenarioTest : ScenarioTestBase() {

    init {
        context("Time Ebb puts creature on top of owner's library") {
            test("target creature is put on top of its owner's library") {
                // Setup:
                // - Player 1 has Grizzly Bears on battlefield
                // - Player 2 has Time Ebb in hand with 3 Islands (enough mana for {2}{U})
                // - It's Player 2's main phase with priority
                val game = scenario()
                    .withPlayers("BearsOwner", "TimeEbbCaster")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // Player 1's creature
                    .withCardInHand(2, "Time Ebb")               // Player 2's spell
                    .withLandsOnBattlefield(2, "Island", 3)     // Enough mana for {2}{U}
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Record initial state
                val bearsId = game.findPermanent("Grizzly Bears")
                    ?: error("Grizzly Bears should be on battlefield")
                val libraryBefore = game.librarySize(1)

                // Player 2 casts Time Ebb targeting the bears
                val castResult = game.castSpell(2, "Time Ebb", bearsId)
                withClue("Time Ebb should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Both players pass priority to resolve
                game.resolveStack()

                // Verify results
                withClue("Grizzly Bears should NOT be on battlefield after Time Ebb") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }

                withClue("Grizzly Bears should NOT be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                withClue("Player 1's library size should increase by 1 (creature put on top)") {
                    game.librarySize(1) shouldBe libraryBefore + 1
                }
            }

            test("creature is put on top, not bottom, of library") {
                // Setup with cards in library to verify top placement
                val game = scenario()
                    .withPlayers("BearsOwner", "TimeEbbCaster")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")  // A card already in library
                    .withCardInHand(2, "Time Ebb")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")
                    ?: error("Grizzly Bears should be on battlefield")

                // Cast and resolve Time Ebb
                game.castSpell(2, "Time Ebb", bearsId)
                game.resolveStack()

                // The bears should be on top of the library (first position)
                // We verify by checking that if we were to draw, we'd get the bears
                val topOfLibrary = game.state.getLibrary(game.player1Id).firstOrNull()
                val topCard = topOfLibrary?.let { game.state.getEntity(it) }
                    ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()

                withClue("Top card of library should be Grizzly Bears") {
                    topCard?.name shouldBe "Grizzly Bears"
                }
            }
        }
    }
}
