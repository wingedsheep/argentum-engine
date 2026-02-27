package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ancient Ooze.
 *
 * Card reference:
 * - Ancient Ooze (5GG): *|* Creature — Ooze
 *   Ancient Ooze's power and toughness are each equal to the total mana value
 *   of other creatures you control.
 */
class AncientOozeScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Ancient Ooze CDA power/toughness") {
            test("is 0/0 with no other creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ancient Ooze")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientOoze = game.findPermanent("Ancient Ooze")!!
                val projected = stateProjector.project(game.state)

                withClue("Ancient Ooze should be 0/0 with no other creatures") {
                    projected.getPower(ancientOoze) shouldBe 0
                    projected.getToughness(ancientOoze) shouldBe 0
                }
            }

            test("counts total mana value of other creatures you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ancient Ooze") // MV 7
                    .withCardOnBattlefield(1, "Glory Seeker") // MV 2
                    .withCardOnBattlefield(1, "Elvish Aberration") // MV 6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientOoze = game.findPermanent("Ancient Ooze")!!
                val projected = stateProjector.project(game.state)

                // Glory Seeker (2) + Elvish Aberration (6) = 8
                withClue("Ancient Ooze should be 8/8 with MV 2 + MV 6 other creatures") {
                    projected.getPower(ancientOoze) shouldBe 8
                    projected.getToughness(ancientOoze) shouldBe 8
                }
            }

            test("does not count opponent's creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ancient Ooze")
                    .withCardOnBattlefield(1, "Glory Seeker") // MV 2 (player's)
                    .withCardOnBattlefield(2, "Elvish Aberration") // MV 6 (opponent's)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientOoze = game.findPermanent("Ancient Ooze")!!
                val projected = stateProjector.project(game.state)

                // Only Glory Seeker (2) counts — opponent's Elvish Aberration doesn't
                withClue("Ancient Ooze should only count own creatures") {
                    projected.getPower(ancientOoze) shouldBe 2
                    projected.getToughness(ancientOoze) shouldBe 2
                }
            }

            test("does not count itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ancient Ooze") // MV 7, should NOT be counted
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ancientOoze = game.findPermanent("Ancient Ooze")!!
                val projected = stateProjector.project(game.state)

                // Ancient Ooze should not count its own MV 7
                withClue("Ancient Ooze should not count itself") {
                    projected.getPower(ancientOoze) shouldBe 0
                    projected.getToughness(ancientOoze) shouldBe 0
                }
            }
        }
    }
}
