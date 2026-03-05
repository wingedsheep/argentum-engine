package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hardened Scales:
 * {G}
 * Enchantment
 * If one or more +1/+1 counters would be put on a creature you control,
 * that many plus one +1/+1 counters are put on it instead.
 */
class HardenedScalesScenarioTest : ScenarioTestBase() {

    private fun TestGame.getCounters(name: String): Int {
        val entityId = findPermanent(name) ?: return 0
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Hardened Scales adds extra +1/+1 counter") {

            test("adds extra counter when putting counters via spell effect") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hardened Scales")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 creature
                    .withCardInHand(1, "Dragonscale Boon") // put two +1/+1 counters
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Dragonscale Boon", glorySeeker)
                game.resolveStack()

                // Should have 3 counters (2 + 1 from Hardened Scales)
                game.getCounters("Glory Seeker") shouldBe 3
            }

            test("applies to creatures entering with +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hardened Scales")
                    .withCardInHand(1, "Swarm of Bloodflies") // enters with 2 +1/+1 counters
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Swarm of Bloodflies")
                game.resolveStack()

                // Should have 3 counters (2 + 1 from Hardened Scales)
                game.getCounters("Swarm of Bloodflies") shouldBe 3
            }

            test("does not affect opponent's creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hardened Scales")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInHand(2, "Dragonscale Boon")
                    .withLandsOnBattlefield(2, "Forest", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                game.castSpell(2, "Dragonscale Boon", glorySeeker)
                game.resolveStack()

                // Should have only 2 counters (Hardened Scales is P1's, not P2's)
                game.getCounters("Glory Seeker") shouldBe 2
            }

            test("multiple Hardened Scales stack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hardened Scales")
                    .withCardOnBattlefield(1, "Hardened Scales")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Dragonscale Boon")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                game.castSpell(1, "Dragonscale Boon", glorySeeker)
                game.resolveStack()

                // Should have 4 counters (2 + 1 + 1 from two Hardened Scales)
                game.getCounters("Glory Seeker") shouldBe 4
            }
        }
    }
}
