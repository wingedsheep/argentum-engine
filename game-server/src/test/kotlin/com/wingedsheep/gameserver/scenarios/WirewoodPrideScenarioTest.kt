package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wirewood Pride.
 *
 * Card reference:
 * - Wirewood Pride ({G}): Instant
 *   "Target creature gets +X/+X until end of turn, where X is the number
 *   of Elves on the battlefield."
 */
class WirewoodPrideScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Wirewood Pride") {

            test("gives +X/+X where X is number of Elves on battlefield") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Wirewood Pride")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Elvish Warrior")  // Elf #1
                    .withCardOnBattlefield(1, "Wirewood Elf")    // Elf #2
                    .withCardOnBattlefield(1, "Elvish Pioneer")  // Elf #3
                    .withCardOnBattlefield(2, "Glory Seeker")    // Non-Elf target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Target Glory Seeker (2/2) — with 3 Elves it should become 5/5
                val targetId = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Wirewood Pride", targetId)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 5/5 with 3 Elves on battlefield (+3/+3)") {
                    projected.getPower(targetId) shouldBe 5
                    projected.getToughness(targetId) shouldBe 5
                }
            }

            test("gives +0/+0 when no Elves on battlefield") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Wirewood Pride")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 non-Elf, also the target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Wirewood Pride", targetId)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should still be 2/2 with no Elves (+0/+0)") {
                    projected.getPower(targetId) shouldBe 2
                    projected.getToughness(targetId) shouldBe 2
                }
            }

            test("counts opponent's Elves too") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Wirewood Pride")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf #1 (ours)
                    .withCardOnBattlefield(2, "Elvish Pioneer") // Elf #2 (opponent's)
                    .withCardOnBattlefield(1, "Glory Seeker")   // Target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Wirewood Pride", targetId)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 4/4 with 2 Elves on battlefield (+2/+2)") {
                    projected.getPower(targetId) shouldBe 4
                    projected.getToughness(targetId) shouldBe 4
                }
            }

            test("can target an Elf (which counts itself)") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Wirewood Pride")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3 Elf, also the target
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Elvish Warrior")!!
                game.castSpell(1, "Wirewood Pride", targetId)
                game.resolveStack()

                val projected = stateProjector.project(game.state)
                withClue("Elvish Warrior should be 3/4 with 1 Elf (+1/+1)") {
                    projected.getPower(targetId) shouldBe 3
                    projected.getToughness(targetId) shouldBe 4
                }
            }
        }
    }
}
