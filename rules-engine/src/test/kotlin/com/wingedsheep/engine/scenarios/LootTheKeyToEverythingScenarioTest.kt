package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Loot, the Key to Everything (BIG #21).
 *
 * {G}{U}{R} Legendary Creature — Beast Noble 1/2. Ward {1}.
 *   At the beginning of your upkeep, exile the top X cards of your library, where X is the
 *   number of card types among other nonland permanents you control. You may play those cards
 *   this turn.
 */
class LootTheKeyToEverythingScenarioTest : ScenarioTestBase() {

    private fun TestGame.exileCount(playerNumber: Int): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).size
    }

    init {
        test("upkeep exiles X cards where X = distinct card types among other nonland permanents") {
            // Other nonland permanents: an artifact (Mind Stone), a plain enchantment
            // (Angelic Shield), and another creature (Grizzly Bears) → 3 distinct card types.
            // Loot itself is a creature but excluded (excludeSelf). Lands don't count.
            // Start on player 2's end step so a single advance lands on player 1's upkeep.
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Loot, the Key to Everything")
                .withCardOnBattlefield(1, "Mind Stone")        // Artifact
                .withCardOnBattlefield(1, "Grizzly Bears")     // Creature
                .withCardOnBattlefield(1, "Angelic Shield")    // Enchantment
                .withLandsOnBattlefield(1, "Forest", 2)        // lands don't add a card type
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 1's upkeep
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            withClue("X = 3 distinct card types (Artifact, Creature, Enchantment) → exile top 3") {
                game.exileCount(1) shouldBe 3
            }
        }

        test("with only lands and Loot, X = 0 and nothing is exiled") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Loot, the Key to Everything")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // player 1's upkeep
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            withClue("only Loot (excluded as self) and lands → X = 0, no cards exiled") {
                game.exileCount(1) shouldBe 0
            }
        }
    }
}
