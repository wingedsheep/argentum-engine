package com.wingedsheep.engine.handlers.effects.regeneration

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import kotlin.reflect.KClass

/**
 * Executor for CantBeRegeneratedEffect.
 * Marks target as unable to regenerate by creating a CantBeRegenerated floating effect.
 *
 * This effect should be applied BEFORE a destroy effect in a composite
 * so that the regeneration shield check during destruction sees the marker.
 */
class CantBeRegeneratedExecutor : EffectExecutor<CantBeRegeneratedEffect> {

    override val effectType: KClass<CantBeRegeneratedEffect> = CantBeRegeneratedEffect::class

    override fun execute(
        state: GameState,
        effect: CantBeRegeneratedEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for can't be regenerated")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.CantBeRegenerated,
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
