package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for ModifyStatsForGroupEffect.
 * "Creatures you control get +X/+Y until end of turn" and similar group pump effects.
 */
class ModifyStatsForGroupExecutor : EffectExecutor<ModifyStatsForGroupEffect> {

    override val effectType: KClass<ModifyStatsForGroupEffect> = ModifyStatsForGroupEffect::class

    override fun execute(
        state: GameState,
        effect: ModifyStatsForGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val events = mutableListOf<EngineGameEvent>()
        val affectedEntities = mutableSetOf<EntityId>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue
            if (!matchesFilter(state, entityId, cardComponent, effect.filter, context)) continue

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

    private fun matchesFilter(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        filter: CreatureGroupFilter,
        context: EffectContext
    ): Boolean {
        val controllerId = state.getEntity(entityId)?.get<ControllerComponent>()?.playerId

        return when (filter) {
            is CreatureGroupFilter.All -> true
            is CreatureGroupFilter.AllOther -> entityId != context.sourceId
            is CreatureGroupFilter.AllYouControl -> controllerId == context.controllerId
            is CreatureGroupFilter.AllOpponentsControl -> controllerId != context.controllerId
            is CreatureGroupFilter.NonWhite -> !cardComponent.colors.contains(Color.WHITE)
            is CreatureGroupFilter.NotColor -> !cardComponent.colors.contains(filter.excludedColor)
            is CreatureGroupFilter.ColorYouControl ->
                controllerId == context.controllerId && cardComponent.colors.contains(filter.color)
            is CreatureGroupFilter.WithKeywordYouControl ->
                controllerId == context.controllerId && cardComponent.baseKeywords.contains(filter.keyword)
        }
    }
}
