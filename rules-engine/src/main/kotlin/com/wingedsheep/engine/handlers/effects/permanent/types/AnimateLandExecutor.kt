package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import kotlin.reflect.KClass

/**
 * Executor for AnimateLandEffect.
 * "Target land becomes a 1/1 creature until end of turn. It's still a land."
 *
 * Creates two floating effects:
 * 1. Layer.TYPE with AddType("Creature") to make the land a creature
 * 2. Layer.POWER_TOUGHNESS with Sublayer.SET_VALUES to set base P/T
 */
class AnimateLandExecutor : EffectExecutor<AnimateLandEffect> {

    override val effectType: KClass<AnimateLandEffect> = AnimateLandEffect::class

    override fun execute(
        state: GameState,
        effect: AnimateLandEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // Verify the target is still on the battlefield
        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val affectedEntities = setOf(targetId)

        // Floating effect 1: Add "Creature" type on Layer.TYPE
        val addTypeEffect = state.createFloatingEffect(
            layer = Layer.TYPE,
            modification = SerializableModification.AddType("CREATURE"),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        // Floating effect 2: Set base P/T on Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES
        val setPTEffect = state.createFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            sublayer = Sublayer.SET_VALUES,
            modification = SerializableModification.SetPowerToughness(effect.power, effect.toughness),
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context
        )

        val newState = state.addFloatingEffects(listOf(addTypeEffect, setPTEffect))

        return EffectResult.success(newState)
    }
}
