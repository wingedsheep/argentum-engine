package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DiscardContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DiscardCardsEffect
import kotlin.reflect.KClass

/**
 * Executor for DiscardCardsEffect.
 * "Discard X cards" or "Target player discards X cards"
 *
 * If the player has more cards than required to discard, this executor
 * pauses and asks the player to choose which cards to discard.
 * A DiscardContinuation is pushed to resume after the choice.
 */
class DiscardCardsExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<DiscardCardsEffect> {

    override val effectType: KClass<DiscardCardsEffect> = DiscardCardsEffect::class

    override fun execute(
        state: GameState,
        effect: DiscardCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for discard")

        val handZone = ZoneKey(playerId, Zone.HAND)
        val hand = state.getZone(handZone)

        // If hand is empty or has fewer cards than needed, discard all
        if (hand.size <= effect.count) {
            return discardCards(state, playerId, hand)
        }

        // Player must choose which cards to discard
        // Get source name for the prompt
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Create a card selection decision
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
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
            playerId = playerId,
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
