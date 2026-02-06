package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Heedless One.
 *
 * Card reference:
 * - Heedless One (3G): *|* Creature — Elf Avatar
 *   Trample
 *   Heedless One's power and toughness are each equal to the number of Elves on the battlefield.
 */
class HeedlessOneScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Heedless One CDA power/toughness") {
            test("P/T equals number of Elves on the battlefield including itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heedless One")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Another Elf
                    .withCardOnBattlefield(1, "Wirewood Elf")   // Another Elf
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heedlessOne = game.findPermanent("Heedless One")!!

                // 3 Elves on battlefield (Heedless One + Elvish Warrior + Wirewood Elf)
                val projected = stateProjector.project(game.state)
                withClue("Heedless One P/T should be 3/3 with 3 Elves on battlefield") {
                    projected.getPower(heedlessOne) shouldBe 3
                    projected.getToughness(heedlessOne) shouldBe 3
                }
            }

            test("P/T updates when another Elf enters the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heedless One")
                    .withCardInHand(1, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heedlessOne = game.findPermanent("Heedless One")!!

                // Initially just Heedless One = 1 Elf
                val projectedBefore = stateProjector.project(game.state)
                withClue("Heedless One should be 1/1 with only itself") {
                    projectedBefore.getPower(heedlessOne) shouldBe 1
                    projectedBefore.getToughness(heedlessOne) shouldBe 1
                }

                // Cast another Elf
                game.castSpell(1, "Elvish Warrior")
                game.resolveStack()

                // Now 2 Elves on battlefield
                val projectedAfter = stateProjector.project(game.state)
                withClue("Heedless One should be 2/2 with 2 Elves on battlefield") {
                    projectedAfter.getPower(heedlessOne) shouldBe 2
                    projectedAfter.getToughness(heedlessOne) shouldBe 2
                }
            }

            test("counts opponent's Elves too") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Heedless One")
                    .withCardOnBattlefield(2, "Elvish Warrior") // Opponent's Elf
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heedlessOne = game.findPermanent("Heedless One")!!

                // 2 Elves: Heedless One (player 1) + Elvish Warrior (player 2)
                val projected = stateProjector.project(game.state)
                withClue("Heedless One should count opponent's Elves") {
                    projected.getPower(heedlessOne) shouldBe 2
                    projected.getToughness(heedlessOne) shouldBe 2
                }
            }
        }
    }
}
