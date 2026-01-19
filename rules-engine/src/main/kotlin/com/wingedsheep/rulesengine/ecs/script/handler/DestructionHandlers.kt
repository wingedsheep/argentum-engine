package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.DestroyAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.DestroyAllLandsEffect
import com.wingedsheep.rulesengine.ability.DestroyAllLandsOfTypeEffect
import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.ExileEffect
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.DamageComponent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.EcsTarget
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlin.reflect.KClass

/**
 * Handler for DestroyEffect.
 */
class DestroyHandler : BaseEffectHandler<DestroyEffect>() {
    override val effectClass: KClass<DestroyEffect> = DestroyEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DestroyEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return noOp(state)

        return destroyPermanent(state, target.entityId)
    }
}

/**
 * Handler for ExileEffect.
 */
class ExileHandler : BaseEffectHandler<ExileEffect>() {
    override val effectClass: KClass<ExileEffect> = ExileEffect::class

    override fun execute(
        state: EcsGameState,
        effect: ExileEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return noOp(state)

        val container = state.getEntity(target.entityId) ?: return noOp(state)
        val cardComponent = container.get<CardComponent>() ?: return noOp(state)

        val newState = state
            .removeFromZone(target.entityId, ZoneId.BATTLEFIELD)
            .addToZone(target.entityId, ZoneId.EXILE)

        return result(newState, EcsEvent.PermanentExiled(target.entityId, cardComponent.definition.name))
    }
}

/**
 * Handler for ReturnToHandEffect.
 */
class ReturnToHandHandler : BaseEffectHandler<ReturnToHandEffect>() {
    override val effectClass: KClass<ReturnToHandEffect> = ReturnToHandEffect::class

    override fun execute(
        state: EcsGameState,
        effect: ReturnToHandEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<EcsTarget.Permanent>().firstOrNull()
            ?: return noOp(state)

        val container = state.getEntity(target.entityId) ?: return noOp(state)
        val cardComponent = container.get<CardComponent>() ?: return noOp(state)

        val ownerHand = ZoneId(ZoneType.HAND, cardComponent.ownerId)

        val newState = state
            .removeFromZone(target.entityId, ZoneId.BATTLEFIELD)
            .addToZone(target.entityId, ownerHand)

        return result(newState, EcsEvent.PermanentReturnedToHand(target.entityId, cardComponent.definition.name))
    }
}

/**
 * Handler for DestroyAllLandsEffect.
 */
class DestroyAllLandsHandler : BaseEffectHandler<DestroyAllLandsEffect>() {
    override val effectClass: KClass<DestroyAllLandsEffect> = DestroyAllLandsEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DestroyAllLandsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val lands = state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.definition?.isLand == true
        }

        for (landId in lands) {
            val result = destroyPermanent(currentState, landId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }
}

/**
 * Handler for DestroyAllCreaturesEffect.
 */
class DestroyAllCreaturesHandler : BaseEffectHandler<DestroyAllCreaturesEffect>() {
    override val effectClass: KClass<DestroyAllCreaturesEffect> = DestroyAllCreaturesEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DestroyAllCreaturesEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val creatures = state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.definition?.isCreature == true
        }

        for (creatureId in creatures) {
            val result = destroyPermanent(currentState, creatureId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }
}

/**
 * Handler for DestroyAllLandsOfTypeEffect.
 * Destroys all lands of a specific type (e.g., Plains, Islands).
 */
class DestroyAllLandsOfTypeHandler : BaseEffectHandler<DestroyAllLandsOfTypeEffect>() {
    override val effectClass: KClass<DestroyAllLandsOfTypeEffect> = DestroyAllLandsOfTypeEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DestroyAllLandsOfTypeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val targetSubtype = Subtype.of(effect.landType)

        val lands = state.getBattlefield().filter { entityId ->
            val cardComponent = state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
            cardComponent.definition.isLand &&
                cardComponent.definition.typeLine.subtypes.contains(targetSubtype)
        }

        for (landId in lands) {
            val result = destroyPermanent(currentState, landId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }
}

// Shared utility function for destroying permanents
internal fun destroyPermanent(state: EcsGameState, entityId: EntityId): ExecutionResult {
    val container = state.getEntity(entityId) ?: return ExecutionResult(state)
    val cardComponent = container.get<CardComponent>() ?: return ExecutionResult(state)

    val ownerId = cardComponent.ownerId
    val graveyardZone = ZoneId(ZoneType.GRAVEYARD, ownerId)

    val newState = state
        .removeFromZone(entityId, ZoneId.BATTLEFIELD)
        .addToZone(entityId, graveyardZone)
        // Clear damage when moving to graveyard
        .updateEntity(entityId) { c -> c.without<DamageComponent>() }

    val events = mutableListOf<EcsEvent>(
        EcsEvent.PermanentDestroyed(entityId, cardComponent.definition.name)
    )

    if (cardComponent.definition.isCreature) {
        events.add(EcsEvent.CreatureDied(entityId, cardComponent.definition.name, ownerId))
    }

    return ExecutionResult(newState, events)
}
