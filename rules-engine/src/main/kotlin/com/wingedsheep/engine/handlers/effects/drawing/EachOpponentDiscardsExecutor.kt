package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DiscardContinuation
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
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
 * Used by cards like Noxious Toad and Syphon Mind.
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

        // If opponent's hand is empty, nothing to do (and no draw for controller)
        if (hand.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // If hand has fewer or equal cards than needed, discard all
        if (hand.size <= effect.count) {
            val discardResult = discardCards(state, opponentId, hand)
            if (!discardResult.isSuccess) return discardResult

            // Controller draws for each card discarded
            if (effect.controllerDrawsPerDiscard > 0) {
                val drawCount = hand.size * effect.controllerDrawsPerDiscard
                return drawCards(discardResult.state, context.controllerId, drawCount, discardResult.events)
            }

            return discardResult
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
            sourceName = sourceName,
            controllerId = if (effect.controllerDrawsPerDiscard > 0) context.controllerId else null,
            controllerDrawsPerDiscard = effect.controllerDrawsPerDiscard
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

        val cardNames = cardIds.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, cardIds, cardNames))
        )
    }

    companion object {
        /**
         * Draw cards for a player. Used after discard for Syphon Mind-style effects.
         * Also used by ContinuationHandler.resumeDiscard when continuation has draw info.
         */
        fun drawCards(
            state: GameState,
            playerId: EntityId,
            count: Int,
            existingEvents: List<com.wingedsheep.engine.core.GameEvent> = emptyList()
        ): ExecutionResult {
            if (count <= 0) return ExecutionResult.success(state, existingEvents)

            var newState = state
            val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
            val handZone = ZoneKey(playerId, Zone.HAND)
            val drawnCards = mutableListOf<EntityId>()

            repeat(count) {
                val library = newState.getZone(libraryZone)
                if (library.isEmpty()) {
                    newState = newState.updateEntity(playerId) { container ->
                        container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                    }
                    return ExecutionResult.success(
                        newState,
                        existingEvents + listOf(DrawFailedEvent(playerId, "Empty library"))
                    )
                }

                val cardId = library.first()
                drawnCards.add(cardId)
                newState = newState.removeFromZone(libraryZone, cardId)
                newState = newState.addToZone(handZone, cardId)
            }

            val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            return ExecutionResult.success(
                newState,
                existingEvents + listOf(CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames))
            )
        }
    }
}
