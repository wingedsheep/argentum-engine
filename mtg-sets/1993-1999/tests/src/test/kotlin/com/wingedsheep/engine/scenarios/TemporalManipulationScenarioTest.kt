package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.SkipNextTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Temporal Manipulation (P02).
 *
 * Oracle: "Take an extra turn after this one."
 * In a 2-player game the extra turn is modeled by the opponent skipping their next turn.
 */
class TemporalManipulationScenarioTest : ScenarioTestBase() {

    init {
        context("Temporal Manipulation — take an extra turn") {
            test("caster's opponent skips their next turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Temporal Manipulation")
                    .withLandsOnBattlefield(1, "Island", 5) // {3}{U}{U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpell(1, "Temporal Manipulation")
                withClue("Casting should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                game.state.getEntity(game.player2Id)?.has<SkipNextTurnComponent>() shouldBe true
                game.state.getEntity(game.player1Id)?.has<SkipNextTurnComponent>() shouldBe false
            }
        }
    }
}
