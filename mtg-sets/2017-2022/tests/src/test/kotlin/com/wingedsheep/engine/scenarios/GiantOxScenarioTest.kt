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
 * Scenario tests for Giant Ox (KHM #11, reprinted in J22).
 *
 *   This creature crews Vehicles using its toughness rather than its power.
 *
 * The boost is crew-only: alone it crews Dependable Quinjet (crew 4 — its toughness is 6), but it
 * can't saddle Bridled Bighorn (saddle 2 — its power is 0), because saddling still reads its plain power.
 */
class GiantOxScenarioTest : ScenarioTestBase() {

    private fun build(other: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Giant Ox")
        .withCardOnBattlefield(1, other)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Giant Ox") {

            test("crews a crew 4 Vehicle on its own") {
                val game = build("Dependable Quinjet")
                val crewer = game.findPermanent("Giant Ox")!!
                val vehicle = game.findPermanent("Dependable Quinjet")!!

                game.execute(CrewVehicle(game.player1Id, vehicle, listOf(crewer))).error shouldBe null
                game.state.getEntity(crewer)?.has<TappedComponent>() shouldBe true
            }

            test("does not saddle a saddle 2 Mount on its own") {
                val game = build("Bridled Bighorn")
                val saddler = game.findPermanent("Giant Ox")!!
                val mount = game.findPermanent("Bridled Bighorn")!!

                game.execute(SaddleMount(game.player1Id, mount, listOf(saddler))).error shouldNotBe null
                game.state.getEntity(saddler)?.has<TappedComponent>() shouldBe false
            }
        }
    }
}
