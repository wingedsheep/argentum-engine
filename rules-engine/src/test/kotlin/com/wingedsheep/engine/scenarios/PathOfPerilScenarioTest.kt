package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Path of Peril (VOW #124).
 *
 * {1}{B}{B} Sorcery — Cleave {4}{W}{B}
 * "Destroy all creatures [with mana value 2 or less]."
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * cast is a narrow sweeper (only creatures with mana value 2 or less); the cleaved cast is an
 * unconditional wrath. A mass-destroy has no target, so the two modes differ only in the
 * [effect]'s filter — the base filters by mana value, the [cleaveEffect] hits everything.
 *
 * Board: Grizzly Bears (MV 2, {1}{G}) is swept by both modes; Hill Giant (MV 4, {3}{R}) survives
 * the printed sweep but dies to the cleaved one.
 */
class PathOfPerilScenarioTest : ScenarioTestBase() {

    init {
        context("Path of Peril — printed cast (brackets present)") {

            test("destroys only creatures with mana value 2 or less; spares bigger creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Path of Peril")
                    .withLandsOnBattlefield(1, "Swamp", 3) // {1}{B}{B}
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // MV 2
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)     // MV 4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Path of Peril")
                withClue("Casting Path of Peril should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears (MV 2) is destroyed by the narrow sweep") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant (MV 4) survives the printed cast") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }
        }

        context("Path of Peril — cleaved cast (brackets removed)") {

            test("destroys all creatures regardless of mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Path of Peril")
                    .withLandsOnBattlefield(1, "Plains", 4) // partial Cleave {4}{W}{B}
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) // MV 2
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)     // MV 4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellWithCleave(1, "Path of Peril")
                withClue("Casting Path of Peril for its cleave cost should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("Both creatures are destroyed by the unconditional wrath") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }
        }
    }
}
