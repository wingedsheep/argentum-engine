package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.StatsModifiedEvent
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
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import kotlin.reflect.KClass

/**
 * Executor for ModifyStatsEffect.
 * "Target creature gets +X/+Y until end of turn"
 */
class ModifyStatsExecutor : EffectExecutor<ModifyStatsEffect> {

    override val effectType: KClass<ModifyStatsEffect> = ModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: ModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target creature
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for stat modification")

        // Verify target exists and is a creature
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        // Face-down permanents are always creatures (Rule 707.2)
        if (!cardComponent.typeLine.isCreature && !targetContainer.has<FaceDownComponent>()) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // Create a floating effect for the stat modification
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.MODIFICATIONS,
                modification = SerializableModification.ModifyPowerToughness(
                    powerMod = effect.powerModifier,
                    toughnessMod = effect.toughnessModifier
                ),
                affectedEntities = setOf(targetId)
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        // Add the floating effect to game state
        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        // Emit event for visualization
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            StatsModifiedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                powerChange = effect.powerModifier,
                toughnessChange = effect.toughnessModifier,
                sourceName = sourceName
            )
        )

        return ExecutionResult.success(newState, events)
    }
}
