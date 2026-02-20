package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.toEntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.TapTargetCreaturesEffect
import kotlin.reflect.KClass

/**
 * Executor for TapTargetCreaturesEffect.
 * "Tap up to X target creatures" - taps all creatures in context.targets.
 *
 * Used for cards like Tidal Surge: "Tap up to three target creatures without flying."
 * The filtering is handled by the TargetRequirement during target selection.
 */
class TapTargetCreaturesExecutor : EffectExecutor<TapTargetCreaturesEffect> {

    override val effectType: KClass<TapTargetCreaturesEffect> = TapTargetCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: TapTargetCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // Iterate through all chosen targets and tap each one
        for (chosenTarget in context.targets) {
            val entityId = chosenTarget.toEntityId()
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Skip already tapped creatures
            if (container.has<TappedComponent>()) continue

            // Tap the creature
            newState = newState.updateEntity(entityId) { it.with(TappedComponent) }
            events.add(TappedEvent(entityId, cardComponent.name))
        }

        return ExecutionResult.success(newState, events)
    }
}
