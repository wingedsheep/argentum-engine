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
 * Bonepicker Skirge (ONE #86) — {2}{B} 2/2 Phyrexian Imp.
 *
 * "Flying. Corrupted — As long as an opponent has three or more poison counters, this creature
 *  has deathtouch and lifelink."
 */
class BonepickerSkirgeScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun game(opponentPoison: Int, ownPoison: Int = 0): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Bonepicker Skirge")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        if (ownPoison > 0) game.setPoison(game.player1Id, ownPoison)
        return game to game.findPermanent("Bonepicker Skirge")!!
    }

    init {
        test("without corrupted it has only flying") {
            val (game, skirge) = game(opponentPoison = 2)
            val projected = game.state.projectedState
            projected.hasKeyword(skirge, Keyword.FLYING) shouldBe true
            projected.hasKeyword(skirge, Keyword.DEATHTOUCH) shouldBe false
            projected.hasKeyword(skirge, Keyword.LIFELINK) shouldBe false
            projected.getPower(skirge) shouldBe 2
            projected.getToughness(skirge) shouldBe 2
        }

        test("an opponent with three poison counters gives it deathtouch and lifelink") {
            val (game, skirge) = game(opponentPoison = 3)
            val projected = game.state.projectedState
            projected.hasKeyword(skirge, Keyword.FLYING) shouldBe true
            projected.hasKeyword(skirge, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(skirge, Keyword.LIFELINK) shouldBe true
        }

        test("its controller's own poison counters don't count") {
            val (game, skirge) = game(opponentPoison = 0, ownPoison = 5)
            val projected = game.state.projectedState
            projected.hasKeyword(skirge, Keyword.DEATHTOUCH) shouldBe false
            projected.hasKeyword(skirge, Keyword.LIFELINK) shouldBe false
        }
    }
}
