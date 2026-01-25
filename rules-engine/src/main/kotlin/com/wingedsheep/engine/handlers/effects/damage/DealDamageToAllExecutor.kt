package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.DealDamageToAllEffect

/**
 * Executor for DealDamageToAllEffect.
 * "Deal X damage to each creature and each player"
 */
class DealDamageToAllExecutor : EffectExecutor<DealDamageToAllEffect> {

    override fun execute(
        state: GameState,
        effect: DealDamageToAllEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // Damage to creatures
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Filter by flying if specified
            val hasFlying = cardComponent.baseKeywords.any { it.name == "FLYING" }
            if (effect.onlyFlyingCreatures && !hasFlying) continue
            if (effect.onlyNonFlyingCreatures && hasFlying) continue

            val result = dealDamageToTarget(newState, entityId, effect.amount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        // Damage to players
        for (playerId in state.turnOrder) {
            val result = dealDamageToTarget(newState, playerId, effect.amount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
