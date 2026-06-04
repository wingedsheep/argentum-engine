package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.capturePermanentSnapshots
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
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // Find the zone the permanent is in
        val currentZone = state.zones.entries.find { (_, cards) -> targetId in cards }?.key
            ?: return EffectResult.success(state)

        // Must be on the battlefield to sacrifice
        if (currentZone.zoneType != Zone.BATTLEFIELD) {
            return EffectResult.success(state)
        }

        // "This can't be sacrificed" (projected AbilityFlag) — the sacrifice simply doesn't
        // happen. Backs Zurgo, Thunder's Decree keeping its Mobilize Warrior tokens past the
        // end step. The mobilize delayed-sacrifice trigger still fires; it just no-ops here.
        if (state.projectedState.hasKeyword(targetId, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_SACRIFICED)) {
            return EffectResult.success(state)
        }

        val container = state.getEntity(targetId)
            ?: return EffectResult.success(state)

        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.success(state)

        val controllerId = state.projectedState.getController(targetId)
            ?: container.get<ControllerComponent>()?.playerId
            ?: context.controllerId

        // By default "sacrifice it" only sacrifices a permanent the resolving player controls.
        // When sacrificedByItsController is set, the permanent's own controller sacrifices it
        // ("[that creature]'s controller sacrifices it"), so don't gate on the resolver's control.
        if (!effect.sacrificedByItsController && controllerId != context.controllerId) {
            return EffectResult.success(state)
        }

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: controllerId

        // Capture the permanent's characteristics as it last existed on the battlefield
        // (Rule 608.2h) BEFORE the zone change, so a following sibling effect — e.g.
        // "sacrifice it. If you do, you gain life equal to its toughness" — can read its
        // P/T via DynamicAmount.Sacrificed. Composite executors thread this snapshot into
        // the next sub-effect's context.
        val snapshot = capturePermanentSnapshots(listOf(targetId), state.projectedState)

        // Track Food sacrifice before zone transition
        var newState = ZoneTransitionService.trackFoodSacrifice(state, listOf(targetId), controllerId)

        // Delegate zone movement to ZoneTransitionService
        val transitionResult = ZoneTransitionService.moveToZone(
            newState, targetId, Zone.GRAVEYARD, fromZoneKey = currentZone
        )

        val events = mutableListOf<GameEvent>()
        events.add(PermanentsSacrificedEvent(controllerId, listOf(targetId), listOf(cardComponent.name)))
        events.addAll(transitionResult.events)

        return EffectResult.success(transitionResult.state, events)
            .copy(updatedSacrificedPermanents = snapshot)
    }
}
