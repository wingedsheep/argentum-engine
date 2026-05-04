package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class RemnantElementalScenarioTest : ScenarioTestBase() {

    init {
        context("Remnant Elemental — landfall") {
            test("playing a land grants +2/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Remnant Elemental")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elementalId = game.findPermanent("Remnant Elemental")!!

                withClue("Remnant Elemental should start at 0 power") {
                    game.state.projectedState.getPower(elementalId) shouldBe 0
                }

                val mountainId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mountain"
                }!!

                val playResult = game.execute(PlayLand(game.player1Id, mountainId))
                withClue("Playing Mountain should succeed: ${playResult.error}") {
                    playResult.error shouldBe null
                }

                // Resolve the landfall trigger
                game.resolveStack()

                withClue("Landfall should give Remnant Elemental +2/+0 until end of turn") {
                    game.state.projectedState.getPower(elementalId) shouldBe 2
                }
                withClue("Toughness should remain unchanged at 4") {
                    game.state.projectedState.getToughness(elementalId) shouldBe 4
                }
            }
        }
    }
}
