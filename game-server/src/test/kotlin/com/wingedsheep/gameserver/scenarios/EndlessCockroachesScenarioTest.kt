package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Endless Cockroaches.
 *
 * Card reference:
 * - Endless Cockroaches (1BB): Creature — Insect (1/1)
 *   "When Endless Cockroaches dies, return it to its owner's hand."
 */
class EndlessCockroachesScenarioTest : ScenarioTestBase() {

    init {
        context("Endless Cockroaches - returns to hand on death") {
            test("returns to hand when killed in combat") {
                val game = scenario()
                    .withPlayers("Cockroach Player", "Opponent")
                    .withCardOnBattlefield(1, "Endless Cockroaches")
                    .withCardOnBattlefield(2, "Stern Marshal") // A creature that can kill cockroaches
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Player 1 attacks with Endless Cockroaches
                game.declareAttackers(mapOf("Endless Cockroaches" to 2))

                // Advance to declare blockers
                game.passPriority() // P1 passes after declaring attackers
                game.passPriority() // P2 gets priority

                // Player 2 blocks with their creature
                game.declareBlockers(mapOf("Stern Marshal" to listOf("Endless Cockroaches")))

                // Resolve through combat damage
                game.resolveStack()

                // Pass through combat damage
                // Both players pass priority to move through steps
                var iterations = 0
                while (game.state.step != Step.POSTCOMBAT_MAIN && iterations < 20) {
                    val priorityPlayer = game.state.priorityPlayerId ?: break
                    game.execute(com.wingedsheep.engine.core.PassPriority(priorityPlayer))
                    iterations++
                }

                // Endless Cockroaches should be in hand, not graveyard
                withClue("Endless Cockroaches should be in owner's hand after dying") {
                    game.isInHand(1, "Endless Cockroaches") shouldBe true
                }
                withClue("Endless Cockroaches should not be in graveyard") {
                    game.isInGraveyard(1, "Endless Cockroaches") shouldBe false
                }
            }
        }
    }
}
