package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Glistening Dawn — "Incubate X twice, where X is the number of lands you control."
 */
class GlisteningDawnScenarioTest : ScenarioTestBase() {
    init {
        context("Glistening Dawn") {
            test("creates two Incubator tokens, each with X counters where X is lands you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Glistening Dawn")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Glistening Dawn").error shouldBe null
                game.resolveStack()

                val incubators = game.state.getBattlefield(game.player1Id).filter {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Incubator"
                }
                incubators.size shouldBe 2
                incubators.forEach {
                    game.state.getEntity(it)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 5
                }
                game.state.getBattlefield(game.player2Id).none {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Incubator"
                } shouldBe true
            }
        }
    }
}
