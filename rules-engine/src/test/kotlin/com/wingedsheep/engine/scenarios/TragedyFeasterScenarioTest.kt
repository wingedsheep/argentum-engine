package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LifeGainedAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Tragedy Feaster. */
class TragedyFeasterScenarioTest : ScenarioTestBase() {

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
        context("Tragedy Feaster — Infusion end step (sacrifice unless you gained life)") {

            test("did NOT gain life this turn: must sacrifice a permanent of your choice") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tragedy Feaster", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("End-step trigger fires (no life gained) and prompts a sacrifice") {
                    game.getPendingDecision() shouldNotBe null
                }

                // Sacrifice the Forest (a permanent of the controller's choice).
                val forest = game.findPermanent("Forest")!!
                game.selectCards(listOf(forest))
                game.resolveStack()

                withClue("The chosen permanent is sacrificed to the graveyard") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("Tragedy Feaster itself can be kept (the Forest was sacrificed)") {
                    game.isOnBattlefield("Tragedy Feaster") shouldBe true
                }
            }

            test("gained life this turn: trigger does not fire, nothing is sacrificed") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tragedy Feaster", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Mountain") }
                val game = builder.build()

                // Record that the controller gained life this turn — the intervening-if fails,
                // so the trigger never goes on the stack.
                game.state = game.state.updateEntity(game.player1Id) {
                    it.withComponent(LifeGainedAmountThisTurnComponent(2))
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("No sacrifice prompt when you gained life this turn") {
                    game.getPendingDecision() shouldBe null
                }
                withClue("Both lands remain — nothing was sacrificed") {
                    game.isInGraveyard(1, "Forest") shouldBe false
                    game.isInGraveyard(1, "Swamp") shouldBe false
                }
            }
        }
    }
}
