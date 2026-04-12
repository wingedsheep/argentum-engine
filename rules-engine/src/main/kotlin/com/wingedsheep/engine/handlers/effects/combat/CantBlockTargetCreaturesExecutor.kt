package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CantBlockTargetCreaturesEffect
import kotlin.reflect.KClass

/**
 * Executor for CantBlockTargetCreaturesEffect.
 * "X target creatures can't block this turn."
 *
 * Creates a floating effect with SetCantBlock for all targeted creatures.
 * Used by Wave of Indifference.
 */
class CantBlockTargetCreaturesExecutor : EffectExecutor<CantBlockTargetCreaturesEffect> {

    override val effectType: KClass<CantBlockTargetCreaturesEffect> = CantBlockTargetCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: CantBlockTargetCreaturesEffect,
        context: EffectContext
    ): EffectResult {
        val affectedEntities = mutableSetOf<EntityId>()

        for (chosenTarget in context.targets) {
            val entityId = chosenTarget.toEntityId()
            val container = state.getEntity(entityId) ?: continue
            container.get<CardComponent>() ?: continue

            affectedEntities.add(entityId)
        }

        if (affectedEntities.isEmpty()) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.SetCantBlock,
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        return EffectResult.success(newState)
    }
}
