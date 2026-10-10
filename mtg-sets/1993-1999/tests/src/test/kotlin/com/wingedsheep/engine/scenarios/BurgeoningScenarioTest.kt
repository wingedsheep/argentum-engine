package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Burgeoning (STH #102) — "Whenever an opponent plays a land, you may put a land card from your
 * hand onto the battlefield."
 *
 * First card on the opponent-scoped land-play trigger: it fires on an opponent's land *play* and
 * not on its controller's own.
 */
class BurgeoningScenarioTest : ScenarioTestBase() {

    init {
        context("Burgeoning") {

            test("an opponent's land play lets you put a land from hand onto the battlefield") {
                val game = scenario()
                    .withPlayers("Burgeoner", "Opponent")
                    .withCardOnBattlefield(1, "Burgeoning")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findCardsInHand(2, "Mountain").single()
                game.execute(PlayLand(game.player2Id, mountain)).error.shouldBeNull()
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                decision.playerId shouldBe game.player1Id
                val forest = game.findCardsInHand(1, "Forest").single()
                game.submitDecision(CardsSelectedResponse(decision.id, listOf(forest)))
                game.resolveStack()

                withClue("the Forest is now on the battlefield") {
                    game.findPermanents("Forest").size shouldBe 1
                    game.findCardsInHand(1, "Forest").size shouldBe 0
                }
            }

            test("the controller's own land play doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Burgeoner", "Opponent")
                    .withCardOnBattlefield(1, "Burgeoning")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val island = game.findCardsInHand(1, "Island").single()
                game.execute(PlayLand(game.player1Id, island)).error.shouldBeNull()

                withClue("no trigger and no prompt") {
                    game.state.stack.size shouldBe 0
                    game.state.pendingDecision.shouldBeNull()
                    game.findCardsInHand(1, "Forest").size shouldBe 1
                }
            }
        }
    }
}
