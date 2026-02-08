package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Pyromancer.
 *
 * Card reference:
 * - Goblin Pyromancer ({3}{R}): 2/2 Creature — Goblin Wizard
 *   When Goblin Pyromancer enters the battlefield, Goblin creatures get +3/+0 until end of turn.
 *   At the beginning of the end step, destroy all Goblins.
 *
 * Tests:
 * 1. ETB trigger gives all Goblins +3/+0
 * 2. Non-Goblin creatures are not buffed
 * 3. Goblin Pyromancer itself gets the +3/+0 buff
 * 4. At end step, all Goblins (including Pyromancer) are destroyed
 * 5. Non-Goblin creatures survive the end step destruction
 */
class GoblinPyromancerScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Goblin Pyromancer ETB trigger") {

            test("all Goblins get +3/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Pyromancer")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Goblin 1/2
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goblin Pyromancer")
                game.resolveStack()

                val skyRaider = game.findPermanent("Goblin Sky Raider")!!
                val pyromancer = game.findPermanent("Goblin Pyromancer")!!
                val projected = stateProjector.project(game.state)

                withClue("Goblin Sky Raider (1/2) should be 4/2 with +3/+0 buff") {
                    projected.getPower(skyRaider) shouldBe 4
                    projected.getToughness(skyRaider) shouldBe 2
                }

                withClue("Goblin Pyromancer (2/2) should be 5/2 with +3/+0 buff") {
                    projected.getPower(pyromancer) shouldBe 5
                    projected.getToughness(pyromancer) shouldBe 2
                }
            }

            test("non-Goblin creatures are not buffed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Pyromancer")
                    .withCardOnBattlefield(1, "Glory Seeker") // Human Soldier 2/2
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goblin Pyromancer")
                game.resolveStack()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                val projected = stateProjector.project(game.state)

                withClue("Glory Seeker (2/2) should remain 2/2") {
                    projected.getPower(glorySeeker) shouldBe 2
                    projected.getToughness(glorySeeker) shouldBe 2
                }
            }
        }

        context("Goblin Pyromancer end step trigger") {

            test("all Goblins are destroyed at end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Pyromancer")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Goblin 1/2
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goblin Pyromancer")
                game.resolveStack()

                // Verify Goblins are on the battlefield before end step
                game.isOnBattlefield("Goblin Pyromancer") shouldBe true
                game.isOnBattlefield("Goblin Sky Raider") shouldBe true

                // Advance to end step — triggers "destroy all Goblins"
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // All Goblins should be destroyed
                game.isOnBattlefield("Goblin Pyromancer") shouldBe false
                game.isOnBattlefield("Goblin Sky Raider") shouldBe false
                game.isInGraveyard(1, "Goblin Pyromancer") shouldBe true
                game.isInGraveyard(1, "Goblin Sky Raider") shouldBe true
            }

            test("non-Goblin creatures survive the end step destruction") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Pyromancer")
                    .withCardOnBattlefield(1, "Glory Seeker") // Human Soldier 2/2
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goblin Pyromancer")
                game.resolveStack()

                // Advance to end step
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Goblin Pyromancer should be destroyed
                game.isOnBattlefield("Goblin Pyromancer") shouldBe false
                game.isInGraveyard(1, "Goblin Pyromancer") shouldBe true

                // Non-Goblin creature should survive
                game.isOnBattlefield("Glory Seeker") shouldBe true
            }

            test("opponent's Goblins are also destroyed at end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Goblin Pyromancer")
                    .withCardOnBattlefield(2, "Goblin Sky Raider") // Opponent's Goblin
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Goblin Pyromancer")
                game.resolveStack()

                // Opponent's Goblin also gets the buff
                val skyRaider = game.findPermanent("Goblin Sky Raider")!!
                val projected = stateProjector.project(game.state)
                withClue("Opponent's Goblin Sky Raider should also get +3/+0") {
                    projected.getPower(skyRaider) shouldBe 4
                }

                // Advance to end step
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // All Goblins destroyed, including opponent's
                game.isOnBattlefield("Goblin Pyromancer") shouldBe false
                game.isOnBattlefield("Goblin Sky Raider") shouldBe false
                game.isInGraveyard(2, "Goblin Sky Raider") shouldBe true
            }
        }
    }
}
