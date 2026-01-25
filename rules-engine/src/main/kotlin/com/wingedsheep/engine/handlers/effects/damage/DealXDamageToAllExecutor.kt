package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.dealDamageToTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.CreatureDamageFilter
import com.wingedsheep.sdk.scripting.DealXDamageToAllEffect
import kotlin.reflect.KClass

/**
 * Executor for DealXDamageToAllEffect.
 * "Deal X damage to each [filtered creature] and each player" where X is the spell's X value.
 * Used for cards like Hurricane and Earthquake.
 */
class DealXDamageToAllExecutor : EffectExecutor<DealXDamageToAllEffect> {

    override val effectType: KClass<DealXDamageToAllEffect> = DealXDamageToAllEffect::class

    override fun execute(
        state: GameState,
        effect: DealXDamageToAllEffect,
        context: EffectContext
    ): ExecutionResult {
        val xValue = context.xValue ?: 0

        // If X is 0, no damage is dealt
        if (xValue == 0) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // Damage to creatures (if filter is specified)
        if (effect.creatureFilter != null) {
            for (entityId in state.getBattlefield()) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (!cardComponent.typeLine.isCreature) continue

                // Apply creature filter
                val matches = when (val filter = effect.creatureFilter) {
                    CreatureDamageFilter.All -> true
                    is CreatureDamageFilter.WithKeyword -> cardComponent.baseKeywords.contains(filter.keyword)
                    is CreatureDamageFilter.WithoutKeyword -> !cardComponent.baseKeywords.contains(filter.keyword)
                    is CreatureDamageFilter.OfColor -> cardComponent.colors.contains(filter.color)
                    is CreatureDamageFilter.NotOfColor -> !cardComponent.colors.contains(filter.color)
                    null -> false
                }

                if (!matches) continue

                val result = dealDamageToTarget(newState, entityId, xValue, context.sourceId)
                newState = result.newState
                events.addAll(result.events)
            }
        }

        // Damage to players
        if (effect.includePlayers) {
            for (playerId in state.turnOrder) {
                val result = dealDamageToTarget(newState, playerId, xValue, context.sourceId)
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
