package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Vastlands Scavenger // Bind to Life. */
class VastlandsScavengerScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    init {
        context("Vastlands Scavenger // Bind to Life") {
            test("enters prepared; the Bind to Life copy mills seven and reanimates a creature") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Vastlands Scavenger")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Top of library: a creature among lands so the mill puts a creature onto the battlefield.
                builder = builder.withCardInLibrary(1, "Hill Giant")
                repeat(7) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Vastlands Scavenger")
                game.resolveStack()

                val scavenger = game.findPermanent("Vastlands Scavenger")!!
                withClue("Vastlands Scavenger enters prepared") {
                    game.state.getEntity(scavenger)?.get<PreparedComponent>() shouldNotBe null
                }

                val copyId = game.findExileCopy(1, "Vastlands Scavenger")!!
                game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                game.resolveStack()

                // The mandatory "put a creature card from among them" selection — pick Hill Giant.
                if (game.state.pendingDecision != null) {
                    val milledGiant = game.findCardsInGraveyard(1, "Hill Giant").firstOrNull()
                    if (milledGiant != null) game.selectCards(listOf(milledGiant))
                    game.resolveStack()
                }

                withClue("the milled creature is reanimated onto the battlefield") {
                    game.findPermanent("Hill Giant") shouldNotBe null
                }
                withClue("casting the copy unprepares Vastlands Scavenger") {
                    game.state.getEntity(scavenger)?.get<PreparedComponent>() shouldBe null
                }
            }
        }
    }
}
