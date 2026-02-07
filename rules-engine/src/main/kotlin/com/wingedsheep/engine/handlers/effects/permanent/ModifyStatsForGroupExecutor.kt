package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ModifyStatsForGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for ModifyStatsForGroupEffect.
 * "Creatures you control get +X/+Y until end of turn" and similar group pump effects.
 */
class ModifyStatsForGroupExecutor : EffectExecutor<ModifyStatsForGroupEffect> {

    override val effectType: KClass<ModifyStatsForGroupEffect> = ModifyStatsForGroupEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ModifyStatsForGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val events = mutableListOf<EngineGameEvent>()
        val affectedEntities = mutableSetOf<EntityId>()

        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Face-down permanents are always creatures (Rule 707.2)
            if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue

            // Check excludeSelf
            if (filter.excludeSelf && entityId == context.sourceId) continue

            // Apply unified filter
            if (!predicateEvaluator.matches(state, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            affectedEntities.add(entityId)

            events.add(
                StatsModifiedEvent(
                    targetId = entityId,
                    targetName = cardComponent.name,
                    powerChange = effect.powerModifier,
                    toughnessChange = effect.toughnessModifier,
                    sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.MODIFICATIONS,
                modification = SerializableModification.ModifyPowerToughness(
                    powerMod = effect.powerModifier,
                    toughnessMod = effect.toughnessModifier
                ),
                affectedEntities = affectedEntities
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

        return ExecutionResult.success(newState, events)
    }
}
