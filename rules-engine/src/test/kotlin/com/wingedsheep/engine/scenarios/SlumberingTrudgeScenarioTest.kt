package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Slumbering Trudge. */
class SlumberingTrudgeScenarioTest : ScenarioTestBase() {

    private fun stunCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    /** Resolve surveil's keep/bin + top-card ordering decisions (keeping everything on top). */
    private fun resolveSurveil(game: TestGame) {
        var guard = 0
        while (guard++ < 6) {
            when (val pd = game.getPendingDecision()) {
                null -> return
                is ReorderLibraryDecision -> game.submitDecision(OrderedResponse(pd.id, pd.cards))
                else -> game.skipSelection()
            }
            game.resolveStack()
        }
    }

    init {
        context("Slumbering Trudge — stun counters and tapped entry scale with X") {

            test("X = 0: enters with 3 stun counters and enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Slumbering Trudge")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Slumbering Trudge", xValue = 0).error shouldBe null
                game.resolveStack()

                val trudge = game.findPermanent("Slumbering Trudge")!!
                withClue("3 − 0 = 3 stun counters") {
                    stunCounters(game, trudge) shouldBe 3
                }
                withClue("X is 2 or less → enters tapped") {
                    isTapped(game, trudge) shouldBe true
                }
            }

            test("X = 2: enters with 1 stun counter and enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Slumbering Trudge")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Slumbering Trudge", xValue = 2).error shouldBe null
                game.resolveStack()

                val trudge = game.findPermanent("Slumbering Trudge")!!
                withClue("3 − 2 = 1 stun counter") {
                    stunCounters(game, trudge) shouldBe 1
                }
                withClue("X is 2 or less → enters tapped") {
                    isTapped(game, trudge) shouldBe true
                }
            }

            test("X = 3: enters with no stun counters and untapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Slumbering Trudge")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Slumbering Trudge", xValue = 3).error shouldBe null
                game.resolveStack()

                val trudge = game.findPermanent("Slumbering Trudge")!!
                withClue("3 − 3 = 0 stun counters") {
                    stunCounters(game, trudge) shouldBe 0
                }
                withClue("X is 3 (not 2 or less) → enters untapped") {
                    isTapped(game, trudge) shouldBe false
                }
            }
        }
    }
}
