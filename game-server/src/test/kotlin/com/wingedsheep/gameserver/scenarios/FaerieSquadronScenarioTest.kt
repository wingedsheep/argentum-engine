package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Faerie Squadron with kicker.
 *
 * Card reference:
 * - Faerie Squadron ({U}): Creature — Faerie 1/1
 *   Kicker {3}{U}
 *   If this creature was kicked, it enters with two +1/+1 counters on it and with flying.
 */
class FaerieSquadronScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.getCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Faerie Squadron kicker") {

            test("unkicked enters as 1/1 with no counters and no flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Faerie Squadron")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Faerie Squadron")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faerieId = game.findPermanent("Faerie Squadron")!!
                withClue("Unkicked Faerie Squadron should have no counters") {
                    game.getCounters(faerieId) shouldBe 0
                }
                val clientState = game.getClientState(1)
                withClue("Unkicked Faerie Squadron should not have flying") {
                    clientState.cards[faerieId]!!.keywords.contains(Keyword.FLYING) shouldBe false
                }
            }

            test("kicked enters with two +1/+1 counters and flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Faerie Squadron")
                    .withLandsOnBattlefield(1, "Island", 5) // {U} + {3}{U} kicker
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Faerie Squadron"
                }

                val castResult = game.execute(CastSpell(playerId, cardId, wasKicked = true))
                withClue("Kicked cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val faerieId = game.findPermanent("Faerie Squadron")!!
                withClue("Kicked Faerie Squadron should have two +1/+1 counters") {
                    game.getCounters(faerieId) shouldBe 2
                }
                val clientState = game.getClientState(1)
                withClue("Kicked Faerie Squadron should have flying") {
                    clientState.cards[faerieId]!!.keywords.contains(Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
