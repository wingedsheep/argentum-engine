package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Swarm of Bloodflies.
 *
 * Card reference:
 * - Swarm of Bloodflies (4B): 0/0 Creature — Insect
 *   Flying
 *   This creature enters with two +1/+1 counters on it.
 *   Whenever another creature dies, put a +1/+1 counter on this creature.
 */
class SwarmOfBloodfliesScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.getCounters(entityId: com.wingedsheep.sdk.model.EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Swarm of Bloodflies") {
            test("enters with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Swarm of Bloodflies")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Swarm of Bloodflies")
                game.resolveStack()

                val swarm = game.findPermanent("Swarm of Bloodflies")!!
                withClue("Swarm should enter with 2 +1/+1 counters") {
                    game.getCounters(swarm) shouldBe 2
                }
            }

            test("gets +1/+1 counter when another creature dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Swarm of Bloodflies")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Swarm first so it enters with counters
                game.castSpell(1, "Swarm of Bloodflies")
                game.resolveStack()

                val swarm = game.findPermanent("Swarm of Bloodflies")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Swarm should start with 2 counters") {
                    game.getCounters(swarm) shouldBe 2
                }

                // Kill opponent's creature with Volcanic Hammer
                game.castSpell(1, "Volcanic Hammer", bears)
                game.resolveStack() // Resolve hammer, creature dies
                game.resolveStack() // Resolve death trigger

                withClue("Swarm should have gained 1 counter from creature death") {
                    game.getCounters(swarm) shouldBe 3
                }
            }

            test("gets counter when own creature dies") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Swarm of Bloodflies")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Swarm first
                game.castSpell(1, "Swarm of Bloodflies")
                game.resolveStack()

                val swarm = game.findPermanent("Swarm of Bloodflies")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                // Kill own creature
                game.castSpell(1, "Volcanic Hammer", bears)
                game.resolveStack() // Resolve hammer
                game.resolveStack() // Resolve death trigger

                withClue("Swarm should have gained 1 counter from own creature death") {
                    game.getCounters(swarm) shouldBe 3
                }
            }
        }
    }
}
