package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pelt Collector — {G} Creature — Elf Warrior 1/1.
 *   Whenever another creature you control enters or dies, if that creature's power is greater than
 *   this creature's, put a +1/+1 counter on this creature.
 *   As long as this creature has three or more +1/+1 counters on it, it has trample.
 *
 * Pins the power comparison on both halves of the trigger — the dies half reads the dying
 * creature's last-known power — and the counter-gated trample.
 */
class PeltCollectorScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("another creature enters") {
            test("a bigger creature adds a counter; an equal one does not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pelt Collector")
                    .withCardInHand(1, "Grizzly Bears") // 2/2
                    .withCardInHand(1, "Llanowar Elves") // 1/1
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val collector = game.findPermanent("Pelt Collector")!!

                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()
                withClue("power 1 is not greater than power 1") { game.plusOneCounters(collector) shouldBe 0 }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                withClue("power 2 beats power 1") { game.plusOneCounters(collector) shouldBe 1 }
            }
        }

        context("another creature dies") {
            test("a bigger creature dying adds a counter by its last-known power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pelt Collector")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val collector = game.findPermanent("Pelt Collector")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                withClue("the Bears died with last-known power 2, greater than 1") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.plusOneCounters(collector) shouldBe 1
                }
            }
        }

        context("three or more +1/+1 counters") {
            test("grants trample only from the third counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pelt Collector")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val collector = game.findPermanent("Pelt Collector")!!

                game.state = game.state.updateEntity(collector) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
                }
                withClue("two counters: no trample") {
                    game.state.projectedState.hasKeyword(collector, Keyword.TRAMPLE.name) shouldBe false
                }

                game.state = game.state.updateEntity(collector) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
                }
                withClue("three counters: trample") {
                    game.state.projectedState.hasKeyword(collector, Keyword.TRAMPLE.name) shouldBe true
                }
            }
        }
    }
}
