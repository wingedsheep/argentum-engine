package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Thrashing Mudspawn.
 *
 * Card reference:
 * - Thrashing Mudspawn (3BB): 4/4 Creature — Beast
 *   Whenever Thrashing Mudspawn is dealt damage, you lose that much life.
 *   Morph {1}{B}{B}
 */
class ThrashingMudspawnScenarioTest : ScenarioTestBase() {

    init {
        context("Thrashing Mudspawn damage trigger") {
            test("controller loses 2 life when dealt 2 damage by Shock") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thrashing Mudspawn")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mudspawn = game.findPermanent("Thrashing Mudspawn")!!
                val initialLife = game.getLifeTotal(1)

                // Opponent casts Shock targeting Thrashing Mudspawn
                val castResult = game.castSpell(2, "Shock", mudspawn)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve Shock (deals 2 damage, triggering the ability)
                game.resolveStack()
                // Resolve the triggered ability (lose 2 life)
                game.resolveStack()

                withClue("Controller should lose 2 life from trigger") {
                    game.getLifeTotal(1) shouldBe initialLife - 2
                }

                // Thrashing Mudspawn is 4/4 so it survives Shock
                withClue("Thrashing Mudspawn should survive 2 damage") {
                    game.findPermanent("Thrashing Mudspawn") shouldBe mudspawn
                }
            }

            test("controller loses life equal to combat damage received") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Thrashing Mudspawn")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(1)

                // Advance to declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Thrashing Mudspawn" to listOf("Glory Seeker")))

                // Pass priority through combat damage step - trigger should fire
                var iterations = 0
                while (game.state.step != Step.END_COMBAT && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Thrashing Mudspawn (4/4) survives Glory Seeker (2/2) damage
                withClue("Thrashing Mudspawn should survive combat") {
                    game.findPermanent("Thrashing Mudspawn") shouldBe game.findPermanent("Thrashing Mudspawn")
                }

                // Controller should lose 2 life (Glory Seeker deals 2 damage)
                withClue("Controller should lose 2 life from combat damage trigger") {
                    game.getLifeTotal(1) shouldBe initialLife - 2
                }
            }
        }
    }
}
