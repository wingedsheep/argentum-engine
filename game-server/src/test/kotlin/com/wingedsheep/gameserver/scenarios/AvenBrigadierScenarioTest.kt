package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Aven Brigadier.
 *
 * Card reference:
 * - Aven Brigadier ({3}{W}{W}{W}): 3/5 Creature — Bird Soldier
 *   Flying
 *   Other Bird creatures get +1/+1.
 *   Other Soldier creatures get +1/+1.
 *
 * Tests:
 * 1. Other Bird creatures get +1/+1
 * 2. Other Soldier creatures get +1/+1
 * 3. A Bird Soldier gets +2/+2 (both bonuses stack)
 * 4. Aven Brigadier does not buff itself
 * 5. Non-Bird non-Soldier creatures are not affected
 * 6. Opponent's matching creatures also get the bonus
 */
class AvenBrigadierScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Aven Brigadier lord effects") {

            test("Other Bird creatures get +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(1, "Sage Aven") // Bird Wizard, 1/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sageAven = game.findPermanent("Sage Aven")!!
                val projected = stateProjector.project(game.state)

                withClue("Sage Aven (Bird Wizard 1/3) should be 2/4 with Bird lord bonus") {
                    projected.getPower(sageAven) shouldBe 2
                    projected.getToughness(sageAven) shouldBe 4
                }
            }

            test("Other Soldier creatures get +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(1, "Glory Seeker") // Human Soldier, 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                val projected = stateProjector.project(game.state)

                withClue("Glory Seeker (Human Soldier 2/2) should be 3/3 with Soldier lord bonus") {
                    projected.getPower(glorySeeker) shouldBe 3
                    projected.getToughness(glorySeeker) shouldBe 3
                }
            }

            test("Bird Soldier gets +2/+2 from both lord effects") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(1, "Ascending Aven") // Bird Soldier, 3/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ascendingAven = game.findPermanent("Ascending Aven")!!
                val projected = stateProjector.project(game.state)

                withClue("Ascending Aven (Bird Soldier 3/2) should be 5/4 with both +1/+1 bonuses") {
                    projected.getPower(ascendingAven) shouldBe 5
                    projected.getToughness(ascendingAven) shouldBe 4
                }
            }

            test("Aven Brigadier does not buff itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val brigadier = game.findPermanent("Aven Brigadier")!!
                val projected = stateProjector.project(game.state)

                withClue("Aven Brigadier (3/5) should remain 3/5 — says 'other'") {
                    projected.getPower(brigadier) shouldBe 3
                    projected.getToughness(brigadier) shouldBe 5
                }
            }

            test("Non-Bird non-Soldier creature is not affected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(1, "Grizzly Bears") // Bear, 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val projected = stateProjector.project(game.state)

                withClue("Grizzly Bears (Bear 2/2) should remain 2/2") {
                    projected.getPower(bears) shouldBe 2
                    projected.getToughness(bears) shouldBe 2
                }
            }

            test("Opponent's Bird creatures also get +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(2, "Sage Aven") // Opponent's Bird Wizard, 1/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sageAven = game.findPermanent("Sage Aven")!!
                val projected = stateProjector.project(game.state)

                withClue("Opponent's Sage Aven (Bird Wizard 1/3) should be 2/4") {
                    projected.getPower(sageAven) shouldBe 2
                    projected.getToughness(sageAven) shouldBe 4
                }
            }
        }
    }
}
