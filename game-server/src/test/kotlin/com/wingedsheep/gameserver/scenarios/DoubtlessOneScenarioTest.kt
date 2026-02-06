package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Doubtless One.
 *
 * Card reference:
 * - Doubtless One (3W): *|* Creature — Cleric Avatar
 *   Whenever Doubtless One deals damage, you gain that much life.
 *   Doubtless One's power and toughness are each equal to the number of Clerics on the battlefield.
 */
class DoubtlessOneScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Doubtless One CDA power/toughness") {
            test("P/T equals number of Clerics on the battlefield including itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doubtless One")
                    .withCardOnBattlefield(1, "Shepherd of Rot") // Another Cleric
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val doubtlessOne = game.findPermanent("Doubtless One")!!

                // 2 Clerics on battlefield (Doubtless One + Shepherd of Rot)
                val projected = stateProjector.project(game.state)
                withClue("Doubtless One P/T should be 2/2 with 2 Clerics on battlefield") {
                    projected.getPower(doubtlessOne) shouldBe 2
                    projected.getToughness(doubtlessOne) shouldBe 2
                }
            }

            test("counts opponent's Clerics too") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doubtless One")
                    .withCardOnBattlefield(2, "Shepherd of Rot") // Opponent's Cleric
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val doubtlessOne = game.findPermanent("Doubtless One")!!

                val projected = stateProjector.project(game.state)
                withClue("Doubtless One should count opponent's Clerics") {
                    projected.getPower(doubtlessOne) shouldBe 2
                    projected.getToughness(doubtlessOne) shouldBe 2
                }
            }
        }

        context("Doubtless One damage trigger") {
            test("gains life equal to combat damage dealt to player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doubtless One")
                    .withCardOnBattlefield(1, "Cabal Archon") // Another Cleric to pump P/T
                    .withActivePlayer(1)
                    .withLifeTotal(1, 15)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Doubtless One is 2/2 (itself + Cabal Archon)
                val doubtlessOne = game.findPermanent("Doubtless One")!!
                val projected = stateProjector.project(game.state)
                projected.getPower(doubtlessOne) shouldBe 2

                // Move to combat and attack
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Doubtless One" to 2))

                // Pass through declare blockers (opponent declares no blockers)
                game.passPriority() // Active player passes priority after attackers
                game.declareNoBlockers()

                // Pass through to combat damage
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

                // Trigger should fire and resolve - gain 2 life from dealing 2 damage
                game.resolveStack()

                withClue("Player should gain life equal to damage dealt (2)") {
                    game.getLifeTotal(1) shouldBe 17 // 15 + 2
                }
                withClue("Opponent should lose life from combat damage") {
                    game.getLifeTotal(2) shouldBe 18 // 20 - 2
                }
            }
        }
    }
}
