package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.TapAllCreaturesEffect
import kotlin.reflect.KClass

/**
 * Executor for TapAllCreaturesEffect.
 * "Tap all creatures" with various filters (nonwhite, opponents', etc.)
 */
class TapAllCreaturesExecutor : EffectExecutor<TapAllCreaturesEffect> {

    override val effectType: KClass<TapAllCreaturesEffect> = TapAllCreaturesEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: TapAllCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        val filter = effect.filter
        val predicateContext = PredicateContext.fromEffectContext(context)

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Skip already tapped creatures
            if (container.has<TappedComponent>()) continue

            // Check excludeSelf
            if (filter.excludeSelf && entityId == context.sourceId) continue

            // Apply unified filter
            if (!predicateEvaluator.matches(state, entityId, filter.baseFilter, predicateContext)) {
                continue
            }

            // Tap the creature
            newState = newState.updateEntity(entityId) { it.with(TappedComponent) }
            events.add(TappedEvent(entityId, cardComponent.name))
        }

        return ExecutionResult.success(newState, events)
    }
}
