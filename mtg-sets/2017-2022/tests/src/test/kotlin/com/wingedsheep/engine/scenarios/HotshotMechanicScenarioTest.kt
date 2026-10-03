package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Hotshot Mechanic (NEO #16, reprinted in J22).
 *
 *   This creature crews Vehicles as though its power were 2 greater.
 *
 * The boost is crew-only: alone it crews Ballista Charger (crew 3 — 2 + 2), but it can't saddle
 * Caustic Bronco (saddle 3 — its power is 2), because saddling still reads its plain power.
 */
class HotshotMechanicScenarioTest : ScenarioTestBase() {

    private fun build(other: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hotshot Mechanic")
        .withCardOnBattlefield(1, other)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Hotshot Mechanic") {

            test("crews a crew 3 Vehicle on its own") {
                val game = build("Ballista Charger")
                val crewer = game.findPermanent("Hotshot Mechanic")!!
                val vehicle = game.findPermanent("Ballista Charger")!!

                game.execute(CrewVehicle(game.player1Id, vehicle, listOf(crewer))).error shouldBe null
                game.state.getEntity(crewer)?.has<TappedComponent>() shouldBe true
            }

            test("does not saddle a saddle 3 Mount on its own") {
                val game = build("Caustic Bronco")
                val saddler = game.findPermanent("Hotshot Mechanic")!!
                val mount = game.findPermanent("Caustic Bronco")!!

                game.execute(SaddleMount(game.player1Id, mount, listOf(saddler))).error shouldNotBe null
                game.state.getEntity(saddler)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
