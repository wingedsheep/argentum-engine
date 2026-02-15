package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.DiscardHandEffect
import kotlin.reflect.KClass

/**
 * Executor for DiscardHandEffect.
 * Forces a target player to discard their entire hand.
 * No decision is needed since all cards are discarded.
 */
class DiscardHandExecutor : EffectExecutor<DiscardHandEffect> {

    override val effectType: KClass<DiscardHandEffect> = DiscardHandEffect::class

    override fun execute(
        state: GameState,
        effect: DiscardHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for discard hand")

        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        val hand = state.getZone(handZone)

        if (hand.isEmpty()) {
            return ExecutionResult.success(state)
        }

        var newState = state
        for (cardId in hand) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, hand))
        )
    }
}
