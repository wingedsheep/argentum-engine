package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Force Bubble.
 *
 * Card reference:
 * - Force Bubble ({2}{W}{W}): Enchantment
 *   If damage would be dealt to you, put that many depletion counters on
 *   Force Bubble instead.
 *   When there are four or more depletion counters on Force Bubble, sacrifice it.
 *   At the beginning of each end step, remove all depletion counters from
 *   Force Bubble.
 */
class ForceBubbleScenarioTest : ScenarioTestBase() {

    private fun TestGame.getDepletionCounters(name: String): Int {
        val entityId = findPermanent(name) ?: return 0
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.DEPLETION) ?: 0
    }

    init {
        context("Force Bubble replacement effect - damage to counters") {

            test("spell damage to controller puts depletion counters on Force Bubble instead") {
                val game = scenario()
                    .withPlayers("Bubble Player", "Opponent")
                    .withCardOnBattlefield(1, "Force Bubble")
                    .withCardInHand(2, "Shock") // 2 damage to any target
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Shock targeting player 1
                game.castSpellTargetingPlayer(2, "Shock", 1)
                game.resolveStack()

                // Player 1 should NOT have taken damage
                game.getLifeTotal(1) shouldBe 20

                // Force Bubble should have 2 depletion counters
                game.getDepletionCounters("Force Bubble") shouldBe 2
            }

            test("combat damage puts depletion counters on Force Bubble instead") {
                val game = scenario()
                    .withPlayers("Bubble Player", "Attacker")
                    .withCardOnBattlefield(1, "Force Bubble")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat phase
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Player 1 should NOT have taken damage
                game.getLifeTotal(1) shouldBe 20

                // Force Bubble should have 2 depletion counters
                game.getDepletionCounters("Force Bubble") shouldBe 2
            }
        }

        context("Force Bubble sacrifice threshold") {

            test("Force Bubble is sacrificed when it gets 4 or more depletion counters") {
                val game = scenario()
                    .withPlayers("Bubble Player", "Opponent")
                    .withCardOnBattlefield(1, "Force Bubble")
                    .withCardInHand(2, "Shock") // 2 damage
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First Shock - 2 counters
                game.castSpellTargetingPlayer(2, "Shock", 1)
                game.resolveStack()
                game.getDepletionCounters("Force Bubble") shouldBe 2
                game.getLifeTotal(1) shouldBe 20

                // Second Shock - 4 counters total → sacrifice
                game.castSpellTargetingPlayer(2, "Shock", 1)
                game.resolveStack()

                // Force Bubble should be gone (sacrificed)
                game.findPermanent("Force Bubble") shouldBe null

                // Player 1 still took no damage
                game.getLifeTotal(1) shouldBe 20
            }

            test("single large damage exceeding threshold sacrifices Force Bubble") {
                val game = scenario()
                    .withPlayers("Bubble Player", "Attacker")
                    .withCardOnBattlefield(1, "Force Bubble")
                    .withCardOnBattlefield(2, "Wirewood Guardian") // 6/6
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Combat with 6/6 creature
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Wirewood Guardian" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Force Bubble should be sacrificed (6 >= 4 threshold)
                game.findPermanent("Force Bubble") shouldBe null

                // Player should not have taken damage
                game.getLifeTotal(1) shouldBe 20
            }
        }

        context("Force Bubble end step trigger - counter removal") {

            test("depletion counters are removed at end of turn") {
                val game = scenario()
                    .withPlayers("Bubble Player", "Opponent")
                    .withCardOnBattlefield(1, "Force Bubble")
                    .withCardInHand(2, "Spark Spray") // 1 damage
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Spark Spray targeting player 1
                game.castSpellTargetingPlayer(2, "Spark Spray", 1)
                game.resolveStack()

                // Should have 1 depletion counter
                game.getDepletionCounters("Force Bubble") shouldBe 1

                // Pass to end step — trigger goes on stack
                game.passUntilPhase(Phase.ENDING, Step.END)
                // Resolve the end step trigger
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Counters should be removed
                game.getDepletionCounters("Force Bubble") shouldBe 0
            }

            test("Force Bubble survives turn after taking 3 damage due to counter removal") {
                val game = scenario()
                    .withPlayers("Bubble Player", "Opponent")
                    .withCardOnBattlefield(1, "Force Bubble")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Manually add 3 depletion counters (just under threshold)
                val bubble = game.findPermanent("Force Bubble")!!
                val counters = CountersComponent().withAdded(CounterType.DEPLETION, 3)
                game.state = game.state.updateEntity(bubble) { c -> c.with(counters) }

                game.getDepletionCounters("Force Bubble") shouldBe 3

                // Pass to end step — trigger goes on stack
                game.passUntilPhase(Phase.ENDING, Step.END)
                // Resolve the end step trigger
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                // Force Bubble should still be on the battlefield
                game.findPermanent("Force Bubble") shouldBe bubble

                // Counters should be cleared
                game.getDepletionCounters("Force Bubble") shouldBe 0
            }
        }
    }
}
