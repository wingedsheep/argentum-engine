package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneReturnService
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.ZoneReturn
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

class MoveUntilSourceLeavesExecutor(private val zones: ZoneTransitionService) : EffectExecutor<MoveUntilSourceLeavesEffect> {
    override val effectType: KClass<MoveUntilSourceLeavesEffect> = MoveUntilSourceLeavesEffect::class

    override fun execute(state: GameState, effect: MoveUntilSourceLeavesEffect, context: EffectContext): EffectResult {
        if (effect.anchor != EffectTarget.Self) {
            // Another permanent bounds the duration: it must still be on the battlefield now.
            val anchorId = context.resolveTarget(effect.anchor, state) ?: return EffectResult.success(state)
            if (state.logicalZone(anchorId)?.zoneType != Zone.BATTLEFIELD) return EffectResult.success(state)
            val anchor = state.objectRef(anchorId) ?: return EffectResult.success(state)
            return move(state, effect, context, anchor, linkTo = null)
        }
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        // Phasing does not end the source's battlefield visit.
        if (state.logicalZone(sourceId)?.zoneType != Zone.BATTLEFIELD || context.sourceReferenceLost) {
            return EffectResult.success(state)
        }
        val timestamp = state.getEntity(sourceId)?.get<BattlefieldEntryTimestampComponent>()?.timestamp
        if (context.sourceBattlefieldTimestamp != null && context.sourceBattlefieldTimestamp != timestamp) {
            return EffectResult.success(state)
        }
        val source = state.objectRef(sourceId) ?: return EffectResult.success(state)
        val capturedSource = context.objectReferences.source
        if (capturedSource != null && capturedSource != source) return EffectResult.success(state)
        return move(state, effect, context, source, linkTo = sourceId)
    }

    /**
     * Move the target — or, for an [EffectTarget.GroupRef], every member of the group in one
     * simultaneous move — recording a return per moved object for when [anchor]'s battlefield
     * visit ends.
     */
    private fun move(
        state: GameState,
        effect: MoveUntilSourceLeavesEffect,
        context: EffectContext,
        anchor: ObjectRef,
        linkTo: EntityId?
    ): EffectResult {
        val ids = when (val target = effect.target) {
            is EffectTarget.GroupRef ->
                BattlefieldFilterUtils.resolveGroup(state, target.filter, context, zones.predicateEvaluator)
            else -> listOfNotNull(context.resolveTarget(target, state))
        }
        // Each object returns to the zone it left; one already in the destination stays put.
        val previousZones = ids.mapNotNull { id ->
            val zone = state.logicalZone(id)?.zoneType ?: return@mapNotNull null
            if (zone == effect.destination) null else id to zone
        }.toMap()
        if (previousZones.isEmpty()) return EffectResult.success(state)
        val movingRefs = previousZones.keys.mapNotNull { id -> state.objectRef(id)?.let { it to id } }.toMap()
        val options = ZoneEntryOptions(exiledBy = context.controllerId.takeIf { effect.destination == Zone.EXILE })
        val result = if (previousZones.size == 1) {
            zones.moveToZone(state, previousZones.keys.single(), effect.destination, options)
        } else {
            zones.moveToZoneBatch(state, previousZones.keys.toList(), effect.destination, options)
        }
        val moved = result.transitions.mapNotNull { transition ->
            val oldId = movingRefs[transition.oldObject] ?: return@mapNotNull null
            if (transition.cause != com.wingedsheep.engine.core.ZoneTransitionCause.PRIMARY ||
                transition.toZone != effect.destination
            ) return@mapNotNull null
            val newObject = transition.newObject ?: return@mapNotNull null
            if (!result.state.isCurrentObject(newObject)) null else newObject to previousZones.getValue(oldId)
        }
        if (moved.isEmpty()) return EffectResult.success(result.state, result.events)
        // Keep the ordinary linked-exile view for both cards of a departed meld.
        var linked = result.state
        // Only this source's own pile: another anchor's linked abilities must not see the card.
        if (linkTo != null && effect.destination == Zone.EXILE && linked.isCurrentObject(anchor)) {
            for ((card, _) in moved) linked = ZoneMovementUtils.linkExiledToSource(linked, card.entityId, linkTo)
        }
        val recorded = linked.copy(
            zoneReturns = linked.zoneReturns + moved.map { (card, from) -> ZoneReturn(anchor, card, from) }
        )
        // Moving the anchor itself (a group that includes the source) ends the duration as part of
        // the initial move: every object it moved returns at once, the source included.
        val returns = ZoneReturnService.returnDepartedSources(zones, recorded)
        return EffectResult.success(returns.state, result.events + returns.events)
    }
}
