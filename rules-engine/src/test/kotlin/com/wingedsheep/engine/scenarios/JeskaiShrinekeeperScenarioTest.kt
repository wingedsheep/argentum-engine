package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Jeskai Shrinekeeper. */
class JeskaiShrinekeeperScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Jeskai Shrinekeeper") {

            test("combat damage to a player gains 1 life and draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Jeskai Shrinekeeper", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Jeskai Shrinekeeper" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (game.state.step == Step.COMBAT_DAMAGE && !game.hasPendingDecision() && iterations++ < 20) {
                    game.passPriority()
                }
                game.resolveStack()

                withClue("Jeskai Shrinekeeper (3/3) deals 3 combat damage to the defender") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("Controller gains 1 life from the trigger") {
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("Controller draws a card from the trigger") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
