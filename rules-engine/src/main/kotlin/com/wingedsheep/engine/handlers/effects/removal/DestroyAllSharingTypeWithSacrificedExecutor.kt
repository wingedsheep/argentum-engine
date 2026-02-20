package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DestroyAllSharingTypeWithSacrificedEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllSharingTypeWithSacrificedEffect.
 *
 * Looks up the creature types of the sacrificed creature (from context.sacrificedPermanentSubtypes,
 * which were snapshotted at sacrifice time before zone change), then destroys all creatures
 * on the battlefield sharing at least one creature type.
 *
 * Used by Endemic Plague.
 */
class DestroyAllSharingTypeWithSacrificedExecutor : EffectExecutor<DestroyAllSharingTypeWithSacrificedEffect> {

    override val effectType: KClass<DestroyAllSharingTypeWithSacrificedEffect> =
        DestroyAllSharingTypeWithSacrificedEffect::class

    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: DestroyAllSharingTypeWithSacrificedEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get the sacrificed creature
        val sacrificedId = context.sacrificedPermanents.firstOrNull()
            ?: return ExecutionResult.success(state)

        // Use snapshotted subtypes from sacrifice time (accounts for text replacements),
        // falling back to current entity subtypes if snapshot not available
        val sacrificedSubtypes = context.sacrificedPermanentSubtypes[sacrificedId]
            ?: state.getEntity(sacrificedId)?.get<CardComponent>()
                ?.typeLine?.subtypes?.map { it.value }?.toSet()
            ?: return ExecutionResult.success(state)

        if (sacrificedSubtypes.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Destroy all creatures on the battlefield sharing at least one creature type
        // Use projected state to account for text replacements on battlefield creatures
        val projected = stateProjector.project(state)
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) continue

            // Use projected subtypes to account for text-changing effects
            val creatureSubtypes = projected.getSubtypes(entityId)
            if (creatureSubtypes.intersect(sacrificedSubtypes).isEmpty()) continue

            val result = destroyPermanent(newState, entityId, canRegenerate = !effect.noRegenerate)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
