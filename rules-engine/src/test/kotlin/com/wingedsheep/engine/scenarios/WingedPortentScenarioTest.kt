package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Winged Portent (VOW #89).
 *
 * {1}{U}{U} Instant — Cleave {4}{G}{U}
 * "Draw a card for each creature you control [with flying]."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast draws only for your fliers; paying the cleave cost drops the "with flying"
 * restriction and draws for every creature you control.
 *
 * No target — only the count filter differs. The count is evaluated on resolution (CR 608.2). The
 * board is fixed in each test: one flier (Wind Drake) and one grounded creature (Grizzly Bears)
 * you control, so the printed cast draws 1 and the cleaved cast draws 2. A creature an *opponent*
 * controls is never counted (the `Player.You` scope), which the printed test also pins.
 */
class WingedPortentScenarioTest : ScenarioTestBase() {

    init {
        context("Winged Portent — printed cast (brackets present)") {

            test("draws a card for each creature you control with flying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Winged Portent")
                    .withLandsOnBattlefield(1, "Island", 3) // {1}{U}{U}
                    .withCardOnBattlefield(1, "Wind Drake", summoningSickness = false) // flier
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // grounded
                    .withCardOnBattlefield(2, "Storm Crow", summoningSickness = false) // opponent's flier — not counted
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                val cast = game.castSpell(1, "Winged Portent")
                withClue("Casting Winged Portent should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Only your one flier is counted — draw exactly 1 (opponent's flier excluded)") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
            }
        }

        context("Winged Portent — cleaved cast (brackets removed)") {

            test("draws a card for each creature you control, flying or not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Winged Portent")
                    .withLandsOnBattlefield(1, "Forest", 6) // Cleave {4}{G}{U} — 5 Forest + 1 Island below
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(1, "Wind Drake", summoningSickness = false) // flier
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // grounded
                    .withCardOnBattlefield(2, "Storm Crow", summoningSickness = false) // opponent's — not counted
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)

                val cast = game.castSpellWithCleave(1, "Winged Portent")
                withClue("Paying the cleave cost should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Both your creatures are counted — draw exactly 2 (opponent's flier excluded)") {
                    game.librarySize(1) shouldBe libraryBefore - 2
                }
            }
        }
    }
}
