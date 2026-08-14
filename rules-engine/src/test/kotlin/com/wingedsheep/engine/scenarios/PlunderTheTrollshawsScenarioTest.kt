package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Plunder the Trollshaws (HOB #51) — {1}{U} Instant.
 *
 * "Draw a card. If this spell was cast from a graveyard, draw two cards instead.
 *  Flashback {3}{U}"
 *
 * "Instead" replaces the whole draw, so the graveyard cast must draw *two*, not three — the mistake
 * a "draw one, then draw one more" model would make. The flashback leg also has to exile the card
 * afterwards rather than return it to the graveyard.
 */
class PlunderTheTrollshawsScenarioTest : ScenarioTestBase() {

    init {
        context("Plunder the Trollshaws") {

            test("cast from hand draws exactly one card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Plunder the Trollshaws")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.state.getLibrary(game.player1Id).size

                game.castSpell(1, "Plunder the Trollshaws").error shouldBe null
                game.resolveStack()

                withClue("one card drawn on a hand cast") {
                    game.state.getLibrary(game.player1Id).size shouldBe libraryBefore - 1
                    game.handSize(1) shouldBe 1
                }
                withClue("a normal cast puts it into the graveyard, not exile") {
                    game.isInGraveyard(1, "Plunder the Trollshaws") shouldBe true
                }
            }

            test("flashback from the graveyard draws two cards instead and exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Plunder the Trollshaws")
                    .withLandsOnBattlefield(1, "Island", 4) // Flashback {3}{U}
                    .withCardInLibrary(1, "Centaur Courser")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.state.getLibrary(game.player1Id).size

                game.castSpellFromGraveyard(1, "Plunder the Trollshaws").error shouldBe null
                game.resolveStack()

                withClue("'instead' means two, not one and not three") {
                    game.state.getLibrary(game.player1Id).size shouldBe libraryBefore - 2
                    game.handSize(1) shouldBe 2
                }
                withClue("flashback exiles the card afterwards") {
                    game.isInExile(1, "Plunder the Trollshaws") shouldBe true
                    game.isInGraveyard(1, "Plunder the Trollshaws") shouldBe false
                }
            }
        }
    }
}
