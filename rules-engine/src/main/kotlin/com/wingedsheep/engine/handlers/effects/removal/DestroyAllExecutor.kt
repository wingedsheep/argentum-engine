package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DestroyAllEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllEffect.
 *
 * Destroys all permanents on the battlefield matching the given filter,
 * optionally excluding permanents that have a subtype matching any string
 * in a stored string list.
 */
class DestroyAllExecutor : EffectExecutor<DestroyAllEffect> {

    override val effectType: KClass<DestroyAllEffect> = DestroyAllEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllEffect,
        context: EffectContext
    ): ExecutionResult {
        val exceptSubtypes = effect.exceptSubtypesFromStored?.let { key ->
            context.storedStringLists[key]?.map { Subtype(it) }?.toSet()
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check filter
            if (effect.filter == GameObjectFilter.Creature && !cardComponent.typeLine.isCreature) continue

            // Check subtype exclusion
            if (exceptSubtypes != null) {
                val hasExceptedType = cardComponent.typeLine.subtypes.any { it in exceptSubtypes }
                if (hasExceptedType) continue
            }

            // Destroy
            val result = EffectExecutorUtils.destroyPermanent(
                newState, entityId, canRegenerate = effect.canRegenerate
            )
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
