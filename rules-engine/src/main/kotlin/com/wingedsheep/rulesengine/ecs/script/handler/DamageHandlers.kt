package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.components.DamageComponent
import com.wingedsheep.rulesengine.ecs.components.LifeComponent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.EcsTarget
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for DealDamageEffect.
 */
class DealDamageHandler : BaseEffectHandler<DealDamageEffect>() {
    override val effectClass: KClass<DealDamageEffect> = DealDamageEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DealDamageEffect,
        context: ExecutionContext
    ): ExecutionResult {
        return when (effect.target) {
            is EffectTarget.Controller -> dealDamageToPlayer(state, context.controllerId, effect.amount, context.sourceId)
            is EffectTarget.Opponent -> {
                val opponentId = getOpponent(context.controllerId, state)
                dealDamageToPlayer(state, opponentId, effect.amount, context.sourceId)
            }
            is EffectTarget.EachOpponent -> {
                var currentState = state
                val events = mutableListOf<EcsEvent>()
                for (playerId in state.getPlayerIds()) {
                    if (playerId != context.controllerId) {
                        val result = dealDamageToPlayer(currentState, playerId, effect.amount, context.sourceId)
                        currentState = result.state
                        events.addAll(result.events)
                    }
                }
                ExecutionResult(currentState, events)
            }
            is EffectTarget.TargetCreature, is EffectTarget.AnyTarget -> {
                val target = context.targets.firstOrNull()
                when (target) {
                    is EcsTarget.Player -> dealDamageToPlayer(state, target.playerId, effect.amount, context.sourceId)
                    is EcsTarget.Permanent -> dealDamageToCreature(state, target.entityId, effect.amount, context.sourceId)
                    null -> noOp(state)
                }
            }
            else -> noOp(state)
        }
    }

    private fun dealDamageToPlayer(
        state: EcsGameState,
        playerId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        val container = state.getEntity(playerId) ?: return noOp(state)
        val lifeComponent = container.get<LifeComponent>() ?: return noOp(state)

        val newState = state.updateEntity(playerId) { c ->
            c.with(lifeComponent.loseLife(amount))
        }

        return result(newState, EcsEvent.DamageDealtToPlayer(sourceId, playerId, amount))
    }

    private fun dealDamageToCreature(
        state: EcsGameState,
        creatureId: EntityId,
        amount: Int,
        sourceId: EntityId
    ): ExecutionResult {
        val container = state.getEntity(creatureId) ?: return noOp(state)
        val damageComponent = container.get<DamageComponent>() ?: DamageComponent(0)

        val newState = state.updateEntity(creatureId) { c ->
            c.with(damageComponent.addDamage(amount))
        }

        return result(newState, EcsEvent.DamageDealtToCreature(sourceId, creatureId, amount))
    }
}
