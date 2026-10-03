package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Propagator Drone (MH3 #167) — {1}{G} Creature — Eldrazi Drone 2/2
 *
 *   Devoid
 *   Creature tokens you control have evolve. (They see this creature enter.)
 *   {3}{G}: Create a 0/1 colorless Eldrazi Spawn creature token with "Sacrifice this token: Add {C}."
 */
class PropagatorDroneScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.activateDrone() {
        val drone = findPermanent("Propagator Drone")!!
        val abilityId = cardRegistry.getCard("Propagator Drone")!!.activatedAbilities[0].id
        execute(ActivateAbility(playerId = player1Id, sourceId = drone, abilityId = abilityId)).error shouldBe null
        resolveStack()
    }

    init {
        context("Propagator Drone") {
            test("{3}{G} makes a Spawn, which evolves when a bigger creature enters; the nontoken Drone does not") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Propagator Drone", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val drone = game.findPermanent("Propagator Drone")!!

                game.activateDrone()
                val spawns = game.findPermanents("Eldrazi Spawn")
                spawns shouldHaveSize 1
                val spawn = spawns.single()
                withClue("the Spawn's own entry doesn't evolve it") { game.plusOneCounters(spawn) shouldBe 0 }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("a 2/2 is greater than the 0/1 Spawn — it evolves") { game.plusOneCounters(spawn) shouldBe 1 }
                withClue("the Drone isn't a token, so it doesn't have evolve") { game.plusOneCounters(drone) shouldBe 0 }
            }

            test("tokens already on the battlefield see the Drone itself enter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Spawn-Gang Commander")
                    .withCardInHand(1, "Propagator Drone")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Spawn-Gang Commander").error shouldBe null
                game.resolveStack()
                val spawns = game.findPermanents("Eldrazi Spawn")
                spawns shouldHaveSize 3
                withClue("no evolve source yet — the Commander's entry evolves nothing") {
                    spawns.forEach { game.plusOneCounters(it) shouldBe 0 }
                }

                game.castSpell(1, "Propagator Drone").error shouldBe null
                game.resolveStack()

                withClue("each Spawn evolves off the Drone's own entry") {
                    spawns.forEach { game.plusOneCounters(it) shouldBe 1 }
                }
            }

            test("an opponent's tokens don't get evolve") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Propagator Drone", summoningSickness = false)
                    .withCardInHand(2, "Spawn-Gang Commander")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withLandsOnBattlefield(2, "Forest", 5)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Spawn-Gang Commander").error shouldBe null
                game.resolveStack()
                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.findPermanents("Eldrazi Spawn").forEach { game.plusOneCounters(it) shouldBe 0 }
            }
        }
    }
}
