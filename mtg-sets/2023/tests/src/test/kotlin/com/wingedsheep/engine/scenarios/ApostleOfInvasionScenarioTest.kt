package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Apostle of Invasion (ONE #3) — {4}{W}{W} 4/4 Phyrexian Angel.
 *
 * "Flying. Corrupted — As long as an opponent has three or more poison counters, this creature
 *  has double strike."
 */
class ApostleOfInvasionScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(opponentPoison: Int, ownPoison: Int = 0): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Apostle of Invasion")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game to game.findPermanent("Apostle of Invasion")!!
    }

    init {
        test("without corrupted it has only flying") {
            val (game, apostle) = game(opponentPoison = 2)
            val projected = game.state.projectedState
            projected.hasKeyword(apostle, Keyword.FLYING) shouldBe true
            projected.hasKeyword(apostle, Keyword.DOUBLE_STRIKE) shouldBe false
            projected.getPower(apostle) shouldBe 4
            projected.getToughness(apostle) shouldBe 4
        }

        test("an opponent with three poison counters gives it double strike") {
            val (game, apostle) = game(opponentPoison = 3)
            val projected = game.state.projectedState
            projected.hasKeyword(apostle, Keyword.FLYING) shouldBe true
            projected.hasKeyword(apostle, Keyword.DOUBLE_STRIKE) shouldBe true
        }

        test("its controller's own poison counters don't count") {
            val (game, apostle) = game(opponentPoison = 0, ownPoison = 5)
            game.state.projectedState.hasKeyword(apostle, Keyword.DOUBLE_STRIKE) shouldBe false
        }
    }
}
