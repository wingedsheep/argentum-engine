package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Riders of the Mark — "At the beginning of your end step, if this creature attacked this turn,
 * return it to its owner's hand. If you do, create a number of 1/1 white Human Soldier creature
 * tokens equal to its toughness." Verifies the last-known-toughness token count after the bounce.
 */
class RidersOfTheMarkScenarioTest : ScenarioTestBase() {

    init {
        test("after attacking, returns itself and makes tokens equal to its toughness") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Riders of the Mark") // 7/4
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Riders of the Mark" to 2))
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            // Riders returned to hand; four 1/1 tokens (toughness 4) remain under player 1's control.
            game.isOnBattlefield("Riders of the Mark") shouldBe false
            val p1 = game.player1Id
            val p1Creatures = game.state.getBattlefield().count { id ->
                game.state.projectedState.getController(id) == p1 && game.state.projectedState.isCreature(id)
            }
            p1Creatures shouldBe 4
        }

        test("does not bounce if it didn't attack") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Riders of the Mark")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            // It didn't attack, so it stays and no tokens are made.
            game.isOnBattlefield("Riders of the Mark") shouldBe true
            val p1 = game.player1Id
            val p1Creatures = game.state.getBattlefield().count { id ->
                game.state.projectedState.getController(id) == p1 && game.state.projectedState.isCreature(id)
            }
            p1Creatures shouldBe 1
        }
    }
}
