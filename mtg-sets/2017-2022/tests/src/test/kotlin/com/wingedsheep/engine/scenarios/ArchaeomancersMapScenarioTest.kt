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
 * Archaeomancer's Map (C21 #12) — "Whenever a land an opponent controls enters, if that player
 * controls more lands than you, you may put a land card from your hand onto the battlefield."
 *
 * Proves the intervening-if reads the entering land's controller (counted after the land
 * entered) against your lands.
 */
class ArchaeomancersMapScenarioTest : ScenarioTestBase() {

    init {
        context("Archaeomancer's Map") {

            test("an opponent who now controls more lands lets you put a land from hand onto the battlefield") {
                val game = scenario()
                    .withPlayers("Mapper", "Opponent")
                    .withCardOnBattlefield(1, "Archaeomancer's Map")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardInHand(1, "Forest")
                    .withCardOnBattlefield(2, "Mountain")
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
                }
            }

            test("no trigger when the opponent doesn't control more lands than you") {
                val game = scenario()
                    .withPlayers("Mapper", "Opponent")
                    .withCardOnBattlefield(1, "Archaeomancer's Map")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardInHand(1, "Forest")
                    .withCardOnBattlefield(2, "Mountain")
                    .withCardInHand(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findCardsInHand(2, "Mountain").single()
                game.execute(PlayLand(game.player2Id, mountain)).error.shouldBeNull()

                withClue("two lands each: no trigger and no prompt") {
                    game.state.stack.size shouldBe 0
                    game.state.pendingDecision.shouldBeNull()
                    game.findCardsInHand(1, "Forest").size shouldBe 1
                }
            }
        }
    }
}
