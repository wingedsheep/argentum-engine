package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Reckless Impulse (VOW #174) — {1}{R} Sorcery.
 *
 *   Exile the top two cards of your library. Until the end of your next turn, you may play those
 *   cards.
 *
 * Exercises the impulse-exile: casting the spell exiles the top two library cards and grants
 * permission to play them until the end of the caster's next turn.
 */
class RecklessImpulseScenarioTest : ScenarioTestBase() {

    init {
        context("Reckless Impulse") {

            test("exiles the top two cards of the library and grants permission to play them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Reckless Impulse")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1 = game.player1Id
                val exileBefore = game.state.getExile(player1).size
                val libraryBefore = game.librarySize(1)

                game.castSpell(1, "Reckless Impulse").error shouldBe null
                game.resolveStack()

                withClue("two cards moved from library to exile") {
                    game.librarySize(1) shouldBe libraryBefore - 2
                    game.state.getExile(player1).size shouldBe exileBefore + 2
                }

                val exiledCards = game.state.getExile(player1)
                withClue("both exiled cards have a may-play permission granted") {
                    game.state.mayPlayPermissions.any { permission ->
                        exiledCards.all { it in permission.cardIds }
                    } shouldBe true
                }
            }
        }
    }
}
