package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Wilt in the Heat. */
class WiltInTheHeatScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    init {
        context("Wilt in the Heat") {
            test("deals 5 damage and exiles the creature instead of letting it die") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wilt in the Heat")
                    .withCardOnBattlefield(2, "Grizzly Bears")  // 2/2 — 5 damage is lethal
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Wilt in the Heat", targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("the creature is exiled, not in the graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.state.getExile(game.player2Id).any {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                    } shouldBe true
                }
            }
        }
    }
}
