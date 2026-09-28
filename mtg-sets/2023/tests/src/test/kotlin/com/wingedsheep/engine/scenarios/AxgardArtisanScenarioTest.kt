package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/** Axgard Artisan — first time each turn +1/+1 counters land on it, create a Treasure. */
class AxgardArtisanScenarioTest : ScenarioTestBase() {
    init {
        test("a backup counter on the Artisan makes one Treasure") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Redcap Heelslasher")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Axgard Artisan")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Redcap Heelslasher").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.findPermanent("Axgard Artisan")!!)).error shouldBe null
            game.resolveStack()
            val treasures = game.state.getBattlefield(game.player1Id)
                .count { game.state.getEntity(it)?.get<CardComponent>()?.name == "Treasure" }
            treasures shouldBe 1
        }
    }
}
