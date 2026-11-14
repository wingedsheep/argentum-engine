package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Each player discards a card" chooses every player's card before any is discarded (CR 101.4),
 * so the first chooser's pick no longer moves a card before the next player's prompt — the
 * selection-fate probe sees nothing leave the hand. The AI must still read the prompt as a
 * discard of its *own* card and keep its best one, and each AI seat chooses from its own hand.
 */
class EachPlayerDiscardsSimultaneouslyAiTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: EntityId): String =
        state.getEntity(id)?.get<CardComponent>()?.name ?: id.toString()

    init {
        test("as the first chooser the AI discards its least valuable card, and so does the second seat") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Miasmic Mummy")
                .withCardInHand(1, "Serra Angel")
                .withCardInHand(1, "Swamp")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardInHand(2, "Shivan Dragon")
                .withCardInHand(2, "Mountain")
                .withLandsOnBattlefield(2, "Mountain", 7)
                .apply { repeat(6) { withCardInLibrary(1, "Swamp") } }
                .apply { repeat(6) { withCardInLibrary(2, "Mountain") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val seats = mapOf(
                game.player1Id to AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE),
                game.player2Id to AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE),
            )
            game.castSpell(1, "Miasmic Mummy").error shouldBe null

            val discarded = mutableListOf<Pair<EntityId, List<String>>>()
            var guard = 0
            while (guard++ < 30) {
                val decision = game.state.pendingDecision
                if (decision != null) {
                    val response = seats.getValue(decision.playerId).respondToDecision(game.state, decision)
                    if (decision is SelectCardsDecision) {
                        discarded += decision.playerId to (response as CardsSelectedResponse).selectedCards.map { game.nameOf(it) }
                    }
                    game.execute(SubmitDecision(decision.playerId, response)).error shouldBe null
                    continue
                }
                if (game.state.stack.isEmpty()) break
                game.resolveStack()
            }

            withClue("$discarded") {
                discarded shouldBe listOf(
                    game.player1Id to listOf("Swamp"),
                    game.player2Id to listOf("Mountain"),
                )
            }
        }
    }
}
