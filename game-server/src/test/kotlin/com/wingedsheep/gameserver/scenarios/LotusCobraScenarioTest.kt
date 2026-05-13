package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class LotusCobraScenarioTest : ScenarioTestBase() {

    init {
        context("Lotus Cobra") {
            test("landfall pauses for color choice and adds one mana of the chosen color") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lotus Cobra")
                    .withCardInHand(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountainId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Mountain"
                }

                game.execute(PlayLand(game.player1Id, mountainId))
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("Landfall should pause for a color choice instead of silently defaulting") {
                    decision.shouldBeInstanceOf<ChooseColorDecision>()
                }
                game.submitDecision(
                    ColorChosenResponse((decision as ChooseColorDecision).id, Color.BLUE)
                )

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                withClue("Chosen color (blue) should be the one added to the pool") {
                    pool.blue shouldBe 1
                }
                withClue("No default-green mana should have been added") {
                    pool.green shouldBe 0
                }
            }
        }
    }
}
