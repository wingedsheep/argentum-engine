package com.wingedsheep.engine.handlers.effects.permanent.stats

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.SetBasePowerToughnessEffect
import kotlin.reflect.KClass

/**
 * Executor for SetBasePowerToughnessEffect.
 * Sets a creature's base power and toughness via a floating continuous effect.
 */
class SetBasePowerToughnessExecutor : EffectExecutor<SetBasePowerToughnessEffect> {

    override val effectType: KClass<SetBasePowerToughnessEffect> = SetBasePowerToughnessEffect::class

    override fun execute(
        state: GameState,
        effect: SetBasePowerToughnessEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val newState = state.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            modification = SerializableModification.SetPowerToughness(effect.power, effect.toughness),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context,
            sublayer = Sublayer.SET_VALUES
        )

        return EffectResult.success(newState)
    }
}
