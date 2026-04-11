package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
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
 *
 * Delegates zone movement to [ZoneTransitionService] for consistent cleanup
 * (stripBattlefieldComponents, cleanupCombatReferences, cleanupReverseAttachmentLink,
 * removeFloatingEffectsTargeting).
 */
class SacrificeTargetExecutor : EffectExecutor<SacrificeTargetEffect> {

    override val effectType: KClass<SacrificeTargetEffect> = SacrificeTargetEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = context.resolveTarget(effect.target)
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

        // Track Food sacrifice before zone transition
        var newState = ZoneTransitionService.trackFoodSacrifice(state, listOf(targetId), controllerId)

        // Delegate zone movement to ZoneTransitionService
        val transitionResult = ZoneTransitionService.moveToZone(
            newState, targetId, Zone.GRAVEYARD, fromZoneKey = currentZone
        )

        val events = mutableListOf<GameEvent>()
        events.add(PermanentsSacrificedEvent(controllerId, listOf(targetId), listOf(cardComponent.name)))
        events.addAll(transitionResult.events)

        return ExecutionResult.success(transitionResult.state, events)
    }
}
