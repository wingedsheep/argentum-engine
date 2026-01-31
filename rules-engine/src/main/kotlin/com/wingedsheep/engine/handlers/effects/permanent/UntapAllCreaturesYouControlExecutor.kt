package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.UntapAllCreaturesYouControlEffect
import kotlin.reflect.KClass

/**
 * Executor for UntapAllCreaturesYouControlEffect.
 * "Untap all creatures you control"
 */
class UntapAllCreaturesYouControlExecutor : EffectExecutor<UntapAllCreaturesYouControlEffect> {

    override val effectType: KClass<UntapAllCreaturesYouControlEffect> = UntapAllCreaturesYouControlEffect::class

    override fun execute(
        state: GameState,
        effect: UntapAllCreaturesYouControlEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Must be a creature
            if (!cardComponent.typeLine.isCreature) continue

            // Must be controlled by the caster
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != context.controllerId) continue

            // Skip already untapped creatures
            if (!container.has<TappedComponent>()) continue

            // Untap the creature
            newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
            events.add(UntappedEvent(entityId, cardComponent.name))
        }

        return ExecutionResult.success(newState, events)
    }
}
