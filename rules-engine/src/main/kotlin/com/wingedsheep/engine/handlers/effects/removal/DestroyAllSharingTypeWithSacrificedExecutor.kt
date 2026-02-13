package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.DestroyAllSharingTypeWithSacrificedEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllSharingTypeWithSacrificedEffect.
 *
 * Looks up the creature types of the sacrificed creature (from context.sacrificedPermanents),
 * then destroys all creatures on the battlefield sharing at least one creature type.
 *
 * Used by Endemic Plague.
 */
class DestroyAllSharingTypeWithSacrificedExecutor : EffectExecutor<DestroyAllSharingTypeWithSacrificedEffect> {

    override val effectType: KClass<DestroyAllSharingTypeWithSacrificedEffect> =
        DestroyAllSharingTypeWithSacrificedEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllSharingTypeWithSacrificedEffect,
        context: EffectContext
    ): ExecutionResult {
        // Get the sacrificed creature
        val sacrificedId = context.sacrificedPermanents.firstOrNull()
            ?: return ExecutionResult.success(state)

        // Get the sacrificed creature's subtypes
        val sacrificedCard = state.getEntity(sacrificedId)?.get<CardComponent>()
            ?: return ExecutionResult.success(state)
        val sacrificedSubtypes = sacrificedCard.typeLine.subtypes.map { it.value }.toSet()

        if (sacrificedSubtypes.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Destroy all creatures on the battlefield sharing at least one creature type
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) continue

            // Check if this creature shares at least one subtype with the sacrificed creature
            val creatureSubtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet()
            if (creatureSubtypes.intersect(sacrificedSubtypes).isEmpty()) continue

            val result = destroyPermanent(newState, entityId, canRegenerate = !effect.noRegenerate)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
