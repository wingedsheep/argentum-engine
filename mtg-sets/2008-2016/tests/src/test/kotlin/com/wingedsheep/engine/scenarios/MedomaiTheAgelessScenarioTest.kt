package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Medomai the Ageless (THS) — {4}{W}{U} 4/4 flyer.
 * "Whenever Medomai deals combat damage to a player, take an extra turn after this one.
 *  Medomai can't attack during extra turns."
 */
class MedomaiTheAgelessScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Medomai", "Opponent")
            .withCardOnBattlefield(1, "Medomai the Ageless")
            .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()

        fun TestGame.connectAndResolve() {
            declareAttackers(mapOf("Medomai the Ageless" to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            resolveStack()
        }

        test("connecting gives an extra turn, during which Medomai can't attack") {
            val game = board()
            val turn = game.state.turnNumber
            game.connectAndResolve()
            game.getLifeTotal(2) shouldBe 16

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("the next turn is Medomai's controller's extra turn") {
                game.state.activePlayerId shouldBe game.player1Id
                game.state.turnNumber shouldBe turn + 1
                game.state.isExtraTurn shouldBe true
            }
            withClue("Medomai can't attack during the extra turn") {
                game.declareAttackers(mapOf("Medomai the Ageless" to 2)).error shouldNotBe null
            }
        }

        test("Medomai attacks again on its controller's next natural turn") {
            val game = board()
            game.connectAndResolve()
            // Extra turn (no attack), then the opponent's turn, then the natural turn.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.isExtraTurn shouldBe true
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player2Id
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("back on a natural turn of Medomai's controller") {
                game.state.activePlayerId shouldBe game.player1Id
                game.state.isExtraTurn shouldBe false
            }
            game.declareAttackers(mapOf("Medomai the Ageless" to 2)).error shouldBe null
        }
    }
}
