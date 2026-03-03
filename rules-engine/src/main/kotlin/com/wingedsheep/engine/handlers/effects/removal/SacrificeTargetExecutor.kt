package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for SacrificeTargetEffect.
 *
 * Sacrifices a specific permanent identified by its target reference.
 * Used in delayed triggers where the exact permanent to sacrifice was
 * determined at ability resolution time (e.g., Skirk Alarmist).
 */
class SacrificeTargetExecutor : EffectExecutor<SacrificeTargetEffect> {

    override val effectType: KClass<SacrificeTargetEffect> = SacrificeTargetEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Find the zone the permanent is in
        val currentZone = state.zones.entries.find { (_, cards) -> targetId in cards }?.key
            ?: return ExecutionResult.success(state)

        // Must be on the battlefield to sacrifice
        if (currentZone.zoneType != Zone.BATTLEFIELD) {
            return ExecutionResult.success(state)
        }

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.success(state)

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.success(state)

        val controllerId = container.get<ControllerComponent>()?.playerId ?: context.controllerId
        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: controllerId
        val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

        var newState = state.removeFromZone(currentZone, targetId)
        newState = newState.addToZone(graveyardZone, targetId)

        // Remove floating effects targeting this permanent
        newState = com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
            .removeFloatingEffectsTargeting(newState, targetId)

        val events = listOf(
            PermanentsSacrificedEvent(controllerId, listOf(targetId), listOf(cardComponent.name)),
            ZoneChangeEvent(
                entityId = targetId,
                entityName = cardComponent.name,
                fromZone = Zone.BATTLEFIELD,
                toZone = Zone.GRAVEYARD,
                ownerId = ownerId
            )
        )

        return ExecutionResult.success(newState, events)
    }
}
