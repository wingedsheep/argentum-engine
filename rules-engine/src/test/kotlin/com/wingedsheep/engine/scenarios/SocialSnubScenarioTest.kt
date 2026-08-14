package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Social Snub. */
class SocialSnubScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    init {
        context("Social Snub") {
            test("each player sacrifices a creature; opponent loses 1 and you gain 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Social Snub")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // your creature (also enables the cast trigger)
                    .withCardOnBattlefield(2, "Hill Giant")     // opponent's creature
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myLifeBefore = game.getLifeTotal(1)
                val oppLifeBefore = game.getLifeTotal(2)

                val cardId = game.findCardsInHand(1, "Social Snub").first()
                game.execute(CastSpell(game.player1Id, cardId)).error shouldBe null
                game.resolveStack()
                // The cast trigger ("you may copy this spell") pauses for a yes/no — decline the copy.
                if (game.state.pendingDecision != null) {
                    game.answerYesNo(false)
                    game.resolveStack()
                }

                withClue("each player sacrificed their only creature") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("each opponent lost 1 life") {
                    game.getLifeTotal(2) shouldBe oppLifeBefore - 1
                }
                withClue("you gained 1 life") {
                    game.getLifeTotal(1) shouldBe myLifeBefore + 1
                }
            }
        }
    }
}
