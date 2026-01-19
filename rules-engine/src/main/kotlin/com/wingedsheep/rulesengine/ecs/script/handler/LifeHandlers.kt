package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GainLifeEffect
import com.wingedsheep.rulesengine.ability.LoseLifeEffect
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ResolvedTarget
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for GainLifeEffect.
 */
class GainLifeHandler : BaseEffectHandler<GainLifeEffect>() {
    override val effectClass: KClass<GainLifeEffect> = GainLifeEffect::class

    override fun execute(
        state: GameState,
        effect: GainLifeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state, context)
        val container = state.getEntity(targetPlayerId) ?: return noOp(state)
        val lifeComponent = container.get<LifeComponent>() ?: return noOp(state)

        val newState = state.updateEntity(targetPlayerId) { c ->
            c.with(lifeComponent.gainLife(effect.amount))
        }

        return result(newState, EffectEvent.LifeGained(targetPlayerId, effect.amount))
    }
}

/**
 * Handler for LoseLifeEffect.
 */
class LoseLifeHandler : BaseEffectHandler<LoseLifeEffect>() {
    override val effectClass: KClass<LoseLifeEffect> = LoseLifeEffect::class

    override fun execute(
        state: GameState,
        effect: LoseLifeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state, context)
        val container = state.getEntity(targetPlayerId) ?: return noOp(state)
        val lifeComponent = container.get<LifeComponent>() ?: return noOp(state)

        val newState = state.updateEntity(targetPlayerId) { c ->
            c.with(lifeComponent.loseLife(effect.amount))
        }

        return result(newState, EffectEvent.LifeLost(targetPlayerId, effect.amount))
    }
}

// Shared utility function for resolving player targets
internal fun resolvePlayerTarget(
    target: EffectTarget,
    controllerId: EntityId,
    state: GameState,
    context: ExecutionContext? = null
): EntityId {
    return when (target) {
        is EffectTarget.Controller -> controllerId
        is EffectTarget.Opponent -> getOpponent(controllerId, state)
        is EffectTarget.TargetController -> {
            // Get the controller of the first target
            val firstTarget = context?.targets?.firstOrNull()
            if (firstTarget is ResolvedTarget.Permanent) {
                val entity = state.getEntity(firstTarget.entityId)
                entity?.get<ControllerComponent>()?.controllerId ?: controllerId
            } else {
                controllerId
            }
        }
        else -> controllerId
    }
}

internal fun getOpponent(playerId: EntityId, state: GameState): EntityId {
    return state.getPlayerIds().first { it != playerId }
}
