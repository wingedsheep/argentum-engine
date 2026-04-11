package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import kotlin.reflect.KClass

/**
 * Executor for ModifyStatsEffect.
 * "Target creature gets +X/+Y until end of turn"
 *
 * Supports both fixed and dynamic amounts via [DynamicAmountEvaluator].
 */
class ModifyStatsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ModifyStatsEffect> {

    override val effectType: KClass<ModifyStatsEffect> = ModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: ModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target creature
        val targetId = context.resolveTarget(effect.target, state)
            ?: return ExecutionResult.error(state, "No valid target for stat modification")

        // Verify target exists and is a creature (use projected types for animated lands etc.)
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        val projected = state.projectedState
        if (!projected.isCreature(targetId) && !targetContainer.has<FaceDownComponent>()) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        val powerMod = amountEvaluator.evaluate(state, effect.powerModifier, context)
        val toughnessMod = amountEvaluator.evaluate(state, effect.toughnessModifier, context)

        // Create a floating effect for the stat modification
        val newState = state.addFloatingEffect(
            layer = Layer.POWER_TOUGHNESS,
            modification = SerializableModification.ModifyPowerToughness(powerMod, toughnessMod),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context,
            sublayer = Sublayer.MODIFICATIONS
        )

        // Emit event for visualization
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            StatsModifiedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                powerChange = powerMod,
                toughnessChange = toughnessMod,
                sourceName = sourceName
            )
        )

        return ExecutionResult.success(newState, events)
    }
}
