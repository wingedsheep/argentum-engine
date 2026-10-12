package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Meria's Outrider (DMU #138) — {4}{R} 4/4 Elf Archer with reach.
 *
 * "Domain — When this creature enters, it deals damage to each opponent equal to the number of
 * basic land types among lands you control."
 *
 * Pins that the entry trigger counts basic land *types* (two Mountains count once, a dual counts
 * twice) and hits the opponent, not its controller.
 */
class MeriasOutriderScenarioTest : ScenarioTestBase() {

    init {
        context("Meria's Outrider") {

            test("enters and deals damage to each opponent equal to domain") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Meria's Outrider")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Contaminated Aquifer") // Land — Island Swamp
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Meria's Outrider").error shouldBe null
                game.resolveStack()

                withClue("Mountain, Forest, Island, Swamp, Plains → 5 damage to the opponent") {
                    game.getLifeTotal(2) shouldBe 15
                }
                withClue("its controller takes none") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("duplicate basic land types count once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Meria's Outrider")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Meria's Outrider").error shouldBe null
                game.resolveStack()

                withClue("five Mountains are one basic land type → 1 damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }
        }
    }
}
