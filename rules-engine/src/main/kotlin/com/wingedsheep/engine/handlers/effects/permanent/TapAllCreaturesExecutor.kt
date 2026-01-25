package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CreatureGroupFilter
import com.wingedsheep.sdk.scripting.TapAllCreaturesEffect

/**
 * Executor for TapAllCreaturesEffect.
 * "Tap all creatures" with various filters (nonwhite, opponents', etc.)
 */
class TapAllCreaturesExecutor : EffectExecutor<TapAllCreaturesEffect> {

    override fun execute(
        state: GameState,
        effect: TapAllCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Skip already tapped creatures
            if (container.has<TappedComponent>()) continue

            // Check filter
            if (!matchesFilter(state, entityId, cardComponent, effect.filter, context)) continue

            // Tap the creature
            newState = newState.updateEntity(entityId) { it.with(TappedComponent) }
            events.add(TappedEvent(entityId, cardComponent.name))
        }

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
