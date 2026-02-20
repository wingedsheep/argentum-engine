package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
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
import com.wingedsheep.sdk.scripting.effects.DynamicModifyStatsEffect
import kotlin.reflect.KClass

/**
 * Executor for DynamicModifyStatsEffect.
 * "Target creature gets -X/-X until end of turn, where X is the number of Zombies on the battlefield."
 *
 * Evaluates the dynamic amounts at resolution time, then creates a floating effect
 * with the resolved values.
 */
class DynamicModifyStatsExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<DynamicModifyStatsEffect> {

    override val effectType: KClass<DynamicModifyStatsEffect> = DynamicModifyStatsEffect::class

    override fun execute(
        state: GameState,
        effect: DynamicModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for stat modification")

        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        if (!cardComponent.typeLine.isCreature) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        val powerMod = amountEvaluator.evaluate(state, effect.powerModifier, context)
        val toughnessMod = amountEvaluator.evaluate(state, effect.toughnessModifier, context)

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.MODIFICATIONS,
                modification = SerializableModification.ModifyPowerToughness(
                    powerMod = powerMod,
                    toughnessMod = toughnessMod
                ),
                affectedEntities = setOf(targetId)
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

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
