package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Slumbering Dragon (M13 #148) — {R} 3/3 Creature — Dragon.
 *
 *   Flying
 *   This creature can't attack or block unless it has five or more +1/+1 counters on it.
 *   Whenever a creature attacks you or a planeswalker you control, put a +1/+1 counter on this creature.
 *
 * Pins that the counter gate reads the Dragon's own counters, that the attack trigger fires once
 * per attacker, and the 2012-07-01 ruling: the counters land before blockers are declared, so a
 * fifth counter lets it block in the very combat that woke it.
 */
class SlumberingDragonScenarioTest : ScenarioTestBase() {

    private val name = "Slumbering Dragon"

    private fun TestGame.plusOneCounters(): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Slumbering Dragon") {

            test("can't attack with no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, name)
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf(name to 2)).error shouldNotBe null
            }

            test("one attacker adds one counter, which isn't enough to block") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, name)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.resolveStack()
                game.plusOneCounters() shouldBe 1

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("one counter is short of five") {
                    game.declareBlockers(mapOf(name to listOf("Grizzly Bears"))).error shouldNotBe null
                }
            }

            test("five attackers wake it up before blockers, so it blocks that same combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, name)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf(
                        "Grizzly Bears" to 1,
                        "Savannah Lions" to 1,
                        "Hill Giant" to 1,
                        "Craw Wurm" to 1,
                        "Raging Goblin" to 1,
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("one trigger per attacking creature") {
                    game.plusOneCounters() shouldBe 5
                }
                val dragon = game.findPermanent(name)!!
                game.state.projectedState.getPower(dragon) shouldBe 8
                game.state.projectedState.getToughness(dragon) shouldBe 8

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf(name to listOf("Craw Wurm"))).error shouldBe null
            }
        }
    }
}
