package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.DestroyAllCreaturesEffect
import com.wingedsheep.rulesengine.ability.DestroyAllLandsEffect
import com.wingedsheep.rulesengine.ability.DestroyAllLandsOfTypeEffect
import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ExileEffect
import com.wingedsheep.rulesengine.ability.ReturnToHandEffect
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.DamageComponent
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlin.reflect.KClass

class DestroyHandler : BaseEffectHandler<DestroyEffect>() {
    override val effectClass: KClass<DestroyEffect> = DestroyEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<ChosenTarget.Permanent>().firstOrNull()
            ?: return noOp(state)

        return destroyPermanent(state, target.entityId)
    }
}

/**
 * Handler for ExileEffect.
 * Supports exiling Permanents (from battlefield) and Cards (from graveyard/hand/library).
 * Handles multiple targets via explicit binding or implicit context.
 */
class ExileHandler : BaseEffectHandler<ExileEffect>() {
    override val effectClass: KClass<ExileEffect> = ExileEffect::class

    override fun execute(
        state: GameState,
        effect: ExileEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Resolve explicit targets (via index) or implicit targets (legacy behavior)
        val targetsToExile = when (val targetType = effect.target) {
            is EffectTarget.ContextTarget -> context.getTargetsForIndex(targetType.index)
            is EffectTarget.AnyTarget -> context.targets
            is EffectTarget.TargetCardInGraveyard -> context.targets
            else -> emptyList()
        }

        var currentState = state
        val events = mutableListOf<EffectEvent>()

        for (target in targetsToExile) {
            when (target) {
                is ChosenTarget.Permanent -> {
                    // Exile from Battlefield
                    val container = currentState.getEntity(target.entityId) ?: continue
                    val cardComponent = container.get<CardComponent>() ?: continue

                    currentState = currentState
                        .removeFromZone(target.entityId, ZoneId.BATTLEFIELD)
                        .addToZone(target.entityId, ZoneId.EXILE)

                    events.add(EffectEvent.PermanentExiled(target.entityId, cardComponent.definition.name))
                }
                is ChosenTarget.Card -> {
                    // Exile from Graveyard/Hand/Library
                    val container = currentState.getEntity(target.cardId) ?: continue
                    val cardComponent = container.get<CardComponent>() ?: continue

                    // Remove from the specific zone captured in the target
                    currentState = currentState.removeFromZone(target.cardId, target.zoneId)
                    currentState = currentState.addToZone(target.cardId, ZoneId.EXILE)

                    events.add(EffectEvent.CardExiled(target.cardId, cardComponent.definition.name))
                }
                else -> {}
            }
        }

        return result(currentState, events)
    }
}

class ReturnToHandHandler : BaseEffectHandler<ReturnToHandEffect>() {
    override val effectClass: KClass<ReturnToHandEffect> = ReturnToHandEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnToHandEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val target = context.targets.filterIsInstance<ChosenTarget.Permanent>().firstOrNull()
            ?: return noOp(state)

        val container = state.getEntity(target.entityId) ?: return noOp(state)
        val cardComponent = container.get<CardComponent>() ?: return noOp(state)

        val ownerHand = ZoneId.hand(cardComponent.ownerId)

        val newState = state
            .removeFromZone(target.entityId, ZoneId.BATTLEFIELD)
            .addToZone(target.entityId, ownerHand)

        return result(newState, EffectEvent.PermanentReturnedToHand(target.entityId, cardComponent.definition.name))
    }
}

class DestroyAllLandsHandler : BaseEffectHandler<DestroyAllLandsEffect>() {
    override val effectClass: KClass<DestroyAllLandsEffect> = DestroyAllLandsEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllLandsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EffectEvent>()

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

class DestroyAllCreaturesHandler : BaseEffectHandler<DestroyAllCreaturesEffect>() {
    override val effectClass: KClass<DestroyAllCreaturesEffect> = DestroyAllCreaturesEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllCreaturesEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EffectEvent>()

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

class DestroyAllLandsOfTypeHandler : BaseEffectHandler<DestroyAllLandsOfTypeEffect>() {
    override val effectClass: KClass<DestroyAllLandsOfTypeEffect> = DestroyAllLandsOfTypeEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAllLandsOfTypeEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EffectEvent>()

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

internal fun destroyPermanent(state: GameState, entityId: EntityId): ExecutionResult {
    val container = state.getEntity(entityId) ?: return ExecutionResult(state)
    val cardComponent = container.get<CardComponent>() ?: return ExecutionResult(state)

    val ownerId = cardComponent.ownerId
    val graveyardZone = ZoneId.graveyard(ownerId)

    val newState = state
        .removeFromZone(entityId, ZoneId.BATTLEFIELD)
        .addToZone(entityId, graveyardZone)
        .updateEntity(entityId) { c -> c.without<DamageComponent>() }

    val events = mutableListOf<EffectEvent>(
        EffectEvent.PermanentDestroyed(entityId, cardComponent.definition.name)
    )

    if (cardComponent.definition.isCreature) {
        events.add(EffectEvent.CreatureDied(entityId, cardComponent.definition.name, ownerId))
    }

    return ExecutionResult(newState, events)
}
