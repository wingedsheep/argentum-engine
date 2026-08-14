package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Thopter Fabricator (DFT #68).
 *
 * Thopter Fabricator {2}{U} — Artifact — Vehicle 4/4
 * Flying
 * Whenever you draw your second card each turn, create a 1/1 colorless Thopter artifact creature
 * token with flying.
 * Crew 2
 *
 * The load-bearing claims about "your second card each turn":
 *  - the first draw of a turn does nothing; the second makes exactly one Thopter;
 *  - the third and later draws make nothing more — the trigger is once per turn, not "each draw
 *    after the first";
 *  - only the controller's draws count.
 */
class ThopterFabricatorScenarioTest : ScenarioTestBase() {

    private fun game(drawnAlready: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Thopter Fabricator")
        .withCardsDrawnThisTurn(1, drawnAlready)
        .withCardInHand(1, "Divination")
        .withLandsOnBattlefield(1, "Island", 3)
        .also { builder -> repeat(5) { builder.withCardInLibrary(1, "Grizzly Bears") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Thopter Fabricator") {

            test("drawing the first and second cards of a turn makes exactly one Thopter") {
                val game = game(drawnAlready = 0)
                game.findPermanents("Thopter Token").size shouldBe 0

                // Divination draws two cards: the first crosses no threshold, the second does.
                val result = game.castSpell(1, "Divination")
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("one Thopter for the second card, not one per card drawn") {
                    game.findPermanents("Thopter Token").size shouldBe 1
                }
            }

            test("a later draw in the same turn makes no further Thopters") {
                // Two cards already drawn this turn, so both of Divination's draws are past the
                // second-card threshold.
                val game = game(drawnAlready = 2)

                val result = game.castSpell(1, "Divination")
                withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.findPermanents("Thopter Token").size shouldBe 0
            }
        }
    }
}
