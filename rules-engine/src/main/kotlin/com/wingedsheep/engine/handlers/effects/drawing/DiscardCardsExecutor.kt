package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DiscardCardsEffect

/**
 * Executor for DiscardCardsEffect.
 * "Discard X cards" or "Target player discards X cards"
 */
class DiscardCardsExecutor : EffectExecutor<DiscardCardsEffect> {

    override fun execute(
        state: GameState,
        effect: DiscardCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for discard")

        // For now, discard random cards (proper implementation needs player choice)
        var newState = state
        val discardedCards = mutableListOf<EntityId>()

        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        repeat(effect.count) {
            val hand = newState.getZone(handZone)
            if (hand.isEmpty()) return@repeat

            // Discard first card in hand (simplified - should be player choice)
            val cardId = hand.first()
            discardedCards.add(cardId)

            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, discardedCards))
        )
    }
}
