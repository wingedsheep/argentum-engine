package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DiscardContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EachOpponentDiscardsEffect
import kotlin.reflect.KClass

/**
 * Executor for EachOpponentDiscardsEffect.
 * "Each opponent discards X cards"
 *
 * If an opponent has more cards than required to discard, this executor
 * pauses and asks them to choose which cards to discard.
 * A DiscardContinuation is pushed to resume after the choice.
 *
 * Used by cards like Noxious Toad.
 */
class EachOpponentDiscardsExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<EachOpponentDiscardsEffect> {

    override val effectType: KClass<EachOpponentDiscardsEffect> = EachOpponentDiscardsEffect::class

    override fun execute(
        state: GameState,
        effect: EachOpponentDiscardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val opponentId = context.opponentId
            ?: return ExecutionResult.error(state, "No opponent for each opponent discards effect")

        val handZone = ZoneKey(opponentId, Zone.HAND)
        val hand = state.getZone(handZone)

        // If opponent's hand is empty, nothing to do
        if (hand.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // If hand has fewer or equal cards than needed, discard all
        if (hand.size <= effect.count) {
            return discardCards(state, opponentId, hand)
        }

        // Opponent must choose which cards to discard
        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Create a card selection decision for the opponent
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = opponentId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose ${effect.count} card${if (effect.count > 1) "s" else ""} to discard",
            options = hand,
            minSelections = effect.count,
            maxSelections = effect.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = DiscardContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = opponentId,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Actually discard the specified cards.
     */
    private fun discardCards(
        state: GameState,
        playerId: EntityId,
        cardIds: List<EntityId>
    ): ExecutionResult {
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in cardIds) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, cardIds))
        )
    }
}
