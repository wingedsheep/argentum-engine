package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Scenario tests for Avalanche Tusker.
 *
 * Card reference:
 * - Avalanche Tusker ({2}{G}{U}{R}): 6/4 Creature — Elephant Warrior
 *   Whenever Avalanche Tusker attacks, target creature defending player controls
 *   blocks it this combat if able.
 */
class AvalancheTuskerScenarioTest : ScenarioTestBase() {

    init {
        context("Avalanche Tusker - force block on attack") {

            test("forces target creature to block Avalanche Tusker") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Avalanche Tusker")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3, untapped
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Avalanche Tusker" to 2))

                // Wait for the triggered ability target selection
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have a pending target selection for force block") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Hill Giant as the target to force block
                val hillGiantId = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(hillGiantId))

                // Resolve the triggered ability
                game.resolveStack()

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declaring no blockers should fail — Hill Giant must block Avalanche Tusker
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should fail when force block is active") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must block"
                }

                // Declaring Hill Giant as blocker of Avalanche Tusker should succeed
                val blockResult = game.declareBlockers(mapOf("Hill Giant" to listOf("Avalanche Tusker")))
                withClue("Blocking Avalanche Tusker should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("does not untap tapped creature (unlike Provoke)") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Avalanche Tusker")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hillGiantId = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant should start tapped") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe true
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Avalanche Tusker" to 2))

                // Wait for triggered ability target selection
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                game.selectTargets(listOf(hillGiantId))
                game.resolveStack()

                // Hill Giant should still be tapped (ForceBlock does NOT untap)
                withClue("Hill Giant should remain tapped after force block (not provoke)") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe true
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Tapped creature can't block, so no blockers should succeed
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should succeed when forced creature is tapped: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }
        }
    }
}
