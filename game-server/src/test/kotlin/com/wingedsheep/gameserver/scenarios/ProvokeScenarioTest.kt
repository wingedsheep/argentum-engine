package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.DeclareBlockers
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
 * Scenario tests for Provoke keyword ability.
 *
 * Card reference:
 * - Goblin Grappler ({R}): 1/1 Creature — Goblin
 *   Provoke (Whenever this creature attacks, you may have target creature
 *   defending player controls untap and block it if able.)
 */
class ProvokeScenarioTest : ScenarioTestBase() {

    init {
        context("Provoke keyword") {

            test("provoke untaps target and forces it to block") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Grappler")
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true) // 3/3, tapped
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify Hill Giant starts tapped
                val hillGiantId = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant should start tapped") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe true
                }

                // Move to combat and declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Grappler" to 2))

                // Wait for the provoke trigger target selection
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Should have a pending target selection for provoke") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Hill Giant as the provoke target
                game.selectTargets(listOf(hillGiantId))

                // Resolve the provoke effect
                game.resolveStack()

                // Verify Hill Giant is now untapped
                withClue("Hill Giant should be untapped after provoke") {
                    game.state.getEntity(hillGiantId)?.has<TappedComponent>() shouldBe false
                }

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Declaring no blockers should fail — Hill Giant must block Goblin Grappler
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should fail when provoke forces blocking") {
                    noBlockResult.error shouldNotBe null
                    noBlockResult.error!! shouldContain "must block"
                }

                // Declaring Hill Giant as blocker of Goblin Grappler should succeed
                val blockResult = game.declareBlockers(mapOf("Hill Giant" to listOf("Goblin Grappler")))
                withClue("Blocking Goblin Grappler should succeed: ${blockResult.error}") {
                    blockResult.error shouldBe null
                }
            }

            test("declining provoke does not force blocking") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Grappler")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Grappler" to 2))

                // Wait for the optional provoke trigger target selection
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Decline provoke by skipping targets (optional trigger allows 0 targets)
                game.skipTargets()

                // Resolve stack and advance to declare blockers
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // No blockers should be fine since provoke was declined
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should succeed when provoke was declined: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }

            test("provoke does not force blocking if creature cannot block due to evasion") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Goblin Grappler")
                    .withCardOnBattlefield(2, "Cloud Spirit") // 3/1 flying, can block only flying
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Grappler" to 2))

                // Wait for the optional provoke trigger
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                // Select Cloud Spirit as provoke target
                val cloudSpiritId = game.findPermanent("Cloud Spirit")!!
                game.selectTargets(listOf(cloudSpiritId))
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Cloud Spirit can't block non-flying creature, so no blockers is valid
                val noBlockResult = game.declareNoBlockers()
                withClue("No blockers should succeed when provoked creature can't block non-flyer: ${noBlockResult.error}") {
                    noBlockResult.error shouldBe null
                }
            }
        }
    }
}
