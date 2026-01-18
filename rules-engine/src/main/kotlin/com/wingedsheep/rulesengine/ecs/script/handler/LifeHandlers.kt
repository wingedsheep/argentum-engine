package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for GainLifeEffect.
 */
class GainLifeHandler : BaseEffectHandler<GainLifeEffect>() {
    override val effectClass: KClass<GainLifeEffect> = GainLifeEffect::class

    override fun execute(
        state: EcsGameState,
        effect: GainLifeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        val container = state.getEntity(targetPlayerId) ?: return noOp(state)
        val lifeComponent = container.get<LifeComponent>() ?: return noOp(state)

        val newState = state.updateEntity(targetPlayerId) { c ->
            c.with(lifeComponent.gainLife(effect.amount))
        }

        return result(newState, EcsEvent.LifeGained(targetPlayerId, effect.amount))
    }
}

/**
 * Handler for LoseLifeEffect.
 */
class LoseLifeHandler : BaseEffectHandler<LoseLifeEffect>() {
    override val effectClass: KClass<LoseLifeEffect> = LoseLifeEffect::class

    override fun execute(
        state: EcsGameState,
        effect: LoseLifeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        val container = state.getEntity(targetPlayerId) ?: return noOp(state)
        val lifeComponent = container.get<LifeComponent>() ?: return noOp(state)

        val newState = state.updateEntity(targetPlayerId) { c ->
            c.with(lifeComponent.loseLife(effect.amount))
        }

        return result(newState, EcsEvent.LifeLost(targetPlayerId, effect.amount))
    }
}

// Shared utility function for resolving player targets
internal fun resolvePlayerTarget(target: EffectTarget, controllerId: EntityId, state: EcsGameState): EntityId {
    return when (target) {
        is EffectTarget.Controller -> controllerId
        is EffectTarget.Opponent -> getOpponent(controllerId, state)
        else -> controllerId
    }
}

internal fun getOpponent(playerId: EntityId, state: EcsGameState): EntityId {
    return state.getPlayerIds().first { it != playerId }
}
