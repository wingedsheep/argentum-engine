package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
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

    /** Move the target, recording a return for when [anchor]'s battlefield visit ends. */
    private fun move(
        state: GameState,
        effect: MoveUntilSourceLeavesEffect,
        context: EffectContext,
        anchor: ObjectRef,
        linkTo: EntityId?
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        val previousZone = state.logicalZone(targetId)?.zoneType ?: return EffectResult.success(state)
        if (previousZone == effect.destination) return EffectResult.success(state)
        val result = zones.moveToZone(state, targetId, effect.destination)
        val moved = result.transitions.filter {
            it.oldObject == state.objectRef(targetId) &&
                it.cause == com.wingedsheep.engine.core.ZoneTransitionCause.PRIMARY &&
                it.toZone == effect.destination
        }.mapNotNull { it.newObject }.filter(result.state::isCurrentObject)
        if (result.actualDestination != effect.destination || moved.isEmpty()) {
            return EffectResult.success(result.state, result.events)
        }
        // Keep the ordinary linked-exile view for both cards of a departed meld.
        var linked = result.state
        // Only this source's own pile: another anchor's linked abilities must not see the card.
        if (linkTo != null && effect.destination == Zone.EXILE && linked.isCurrentObject(anchor)) {
            for (card in moved) linked = ZoneMovementUtils.linkExiledToSource(linked, card.entityId, linkTo)
        }
        val recorded = linked.copy(zoneReturns = linked.zoneReturns + moved.map { ZoneReturn(anchor, it, previousZone) })
        // Moving the anchor itself can end the duration as part of the initial move.
        val returns = ZoneReturnService.returnDepartedSources(zones, recorded)
        return EffectResult.success(returns.state, result.events + returns.events)
    }
}
