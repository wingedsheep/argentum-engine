package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.destroyPermanent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.DestroyAllCreaturesEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAllCreaturesEffect.
 * "Destroy all creatures"
 */
class DestroyAllCreaturesExecutor : EffectExecutor<DestroyAllCreaturesEffect> {

    override val effectType: KClass<DestroyAllCreaturesEffect> = DestroyAllCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            val result = destroyPermanent(newState, entityId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
