package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AnimateLandEffect
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
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Verify the target is still on the battlefield
        if (targetId !in state.getBattlefield()) {
            return ExecutionResult.success(state)
        }

        val affectedEntities = setOf(targetId)
        val timestamp = System.currentTimeMillis()
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Floating effect 1: Add "Creature" type on Layer.TYPE
        val addTypeEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                modification = SerializableModification.AddType("CREATURE"),
                affectedEntities = affectedEntities
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            timestamp = timestamp
        )

        // Floating effect 2: Set base P/T on Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES
        val setPTEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.SET_VALUES,
                modification = SerializableModification.SetPowerToughness(effect.power, effect.toughness),
                affectedEntities = affectedEntities
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            timestamp = timestamp
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + addTypeEffect + setPTEffect
        )

        return ExecutionResult.success(newState)
    }
}
