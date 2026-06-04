package com.wingedsheep.engine.handlers.effects.regeneration

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RemoveDamageShieldEffect
import kotlin.reflect.KClass

/**
 * Executor for [RemoveDamageShieldEffect] — places a one-shot destruction shield
 * (Pyramids' second mode). The next time the target would be destroyed this turn,
 * the shield is consumed and the damage marked on it is removed instead. Lasts
 * until end of turn.
 */
class RemoveDamageShieldExecutor : EffectExecutor<RemoveDamageShieldEffect> {

    override val effectType: KClass<RemoveDamageShieldEffect> = RemoveDamageShieldEffect::class

    override fun execute(
        state: GameState,
        effect: RemoveDamageShieldEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "Could not resolve target for RemoveDamageShieldEffect")

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.RemoveDamageShield,
            affectedEntities = setOf(targetId),
            duration = Duration.EndOfTurn,
            context = context
        )

        return EffectResult.success(newState)
    }
}
