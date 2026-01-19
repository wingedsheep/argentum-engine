package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.DealDamageToAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.DealDamageToAllEffect
import com.wingedsheep.rulesengine.ability.DrainEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
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

/**
 * Handler for DealDamageToAllCreaturesEffect.
 * Deals damage to all creatures, optionally filtered by flying/non-flying.
 */
class DealDamageToAllCreaturesHandler : BaseEffectHandler<DealDamageToAllCreaturesEffect>() {
    override val effectClass: KClass<DealDamageToAllCreaturesEffect> = DealDamageToAllCreaturesEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DealDamageToAllCreaturesEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val creatures = state.getBattlefield().filter { entityId ->
            val container = state.getEntity(entityId)
            val cardComponent = container?.get<CardComponent>()
            if (cardComponent?.definition?.isCreature != true) return@filter false

            // Filter by flying status
            val hasFlying = cardComponent.definition.keywords.contains(Keyword.FLYING)
            when {
                effect.onlyFlying -> hasFlying
                effect.onlyNonFlying -> !hasFlying
                else -> true
            }
        }

        for (creatureId in creatures) {
            val container = currentState.getEntity(creatureId)
            val damageComponent = container?.get<DamageComponent>() ?: DamageComponent(0)

            currentState = currentState.updateEntity(creatureId) { c ->
                c.with(damageComponent.addDamage(effect.amount))
            }

            events.add(EcsEvent.DamageDealtToCreature(context.sourceId, creatureId, effect.amount))
        }

        return ExecutionResult(currentState, events)
    }
}

/**
 * Handler for DealDamageToAllEffect.
 * Deals damage to all creatures (optionally filtered) and all players.
 */
class DealDamageToAllHandler : BaseEffectHandler<DealDamageToAllEffect>() {
    override val effectClass: KClass<DealDamageToAllEffect> = DealDamageToAllEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DealDamageToAllEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        // Deal damage to creatures
        val creatures = state.getBattlefield().filter { entityId ->
            val container = state.getEntity(entityId)
            val cardComponent = container?.get<CardComponent>()
            if (cardComponent?.definition?.isCreature != true) return@filter false

            val hasFlying = cardComponent.definition.keywords.contains(Keyword.FLYING)
            when {
                effect.onlyFlyingCreatures -> hasFlying
                effect.onlyNonFlyingCreatures -> !hasFlying
                else -> true
            }
        }

        for (creatureId in creatures) {
            val container = currentState.getEntity(creatureId)
            val damageComponent = container?.get<DamageComponent>() ?: DamageComponent(0)

            currentState = currentState.updateEntity(creatureId) { c ->
                c.with(damageComponent.addDamage(effect.amount))
            }

            events.add(EcsEvent.DamageDealtToCreature(context.sourceId, creatureId, effect.amount))
        }

        // Deal damage to all players
        for (playerId in state.getPlayerIds()) {
            val container = currentState.getEntity(playerId)
            val lifeComponent = container?.get<LifeComponent>() ?: continue

            currentState = currentState.updateEntity(playerId) { c ->
                c.with(lifeComponent.loseLife(effect.amount))
            }

            events.add(EcsEvent.DamageDealtToPlayer(context.sourceId, playerId, effect.amount))
        }

        return ExecutionResult(currentState, events)
    }
}

/**
 * Handler for DrainEffect.
 * Deals damage to target and gains that much life.
 */
class DrainHandler : BaseEffectHandler<DrainEffect>() {
    override val effectClass: KClass<DrainEffect> = DrainEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DrainEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        // Deal damage to target
        when (effect.target) {
            is EffectTarget.AnyTarget, is EffectTarget.TargetCreature -> {
                val target = context.targets.firstOrNull()
                when (target) {
                    is EcsTarget.Player -> {
                        val container = currentState.getEntity(target.playerId)
                        val lifeComponent = container?.get<LifeComponent>() ?: return noOp(state)

                        currentState = currentState.updateEntity(target.playerId) { c ->
                            c.with(lifeComponent.loseLife(effect.amount))
                        }
                        events.add(EcsEvent.DamageDealtToPlayer(context.sourceId, target.playerId, effect.amount))
                    }
                    is EcsTarget.Permanent -> {
                        val container = currentState.getEntity(target.entityId)
                        val damageComponent = container?.get<DamageComponent>() ?: DamageComponent(0)

                        currentState = currentState.updateEntity(target.entityId) { c ->
                            c.with(damageComponent.addDamage(effect.amount))
                        }
                        events.add(EcsEvent.DamageDealtToCreature(context.sourceId, target.entityId, effect.amount))
                    }
                    null -> return noOp(state)
                }
            }
            is EffectTarget.Opponent -> {
                val opponentId = getOpponent(context.controllerId, state)
                val container = currentState.getEntity(opponentId)
                val lifeComponent = container?.get<LifeComponent>() ?: return noOp(state)

                currentState = currentState.updateEntity(opponentId) { c ->
                    c.with(lifeComponent.loseLife(effect.amount))
                }
                events.add(EcsEvent.DamageDealtToPlayer(context.sourceId, opponentId, effect.amount))
            }
            is EffectTarget.TargetNonblackCreature -> {
                val target = context.targets.firstOrNull()
                if (target is EcsTarget.Permanent) {
                    val container = currentState.getEntity(target.entityId)
                    val damageComponent = container?.get<DamageComponent>() ?: DamageComponent(0)

                    currentState = currentState.updateEntity(target.entityId) { c ->
                        c.with(damageComponent.addDamage(effect.amount))
                    }
                    events.add(EcsEvent.DamageDealtToCreature(context.sourceId, target.entityId, effect.amount))
                } else {
                    return noOp(state)
                }
            }
            else -> return noOp(state)
        }

        // Gain life
        val controllerContainer = currentState.getEntity(context.controllerId)
        val controllerLife = controllerContainer?.get<LifeComponent>() ?: return ExecutionResult(currentState, events)

        currentState = currentState.updateEntity(context.controllerId) { c ->
            c.with(controllerLife.gainLife(effect.amount))
        }
        events.add(EcsEvent.LifeGained(context.controllerId, effect.amount))

        return ExecutionResult(currentState, events)
    }
}
