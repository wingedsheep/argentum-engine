package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import kotlin.reflect.KClass

/**
 * Executor for AttachEquipmentEffect.
 * Attaches an equipment to a target creature, detaching from the previous creature if any.
 */
class AttachEquipmentExecutor : EffectExecutor<AttachEquipmentEffect> {

    override val effectType: KClass<AttachEquipmentEffect> = AttachEquipmentEffect::class

    override fun execute(
        state: GameState,
        effect: AttachEquipmentEffect,
        context: EffectContext
    ): EffectResult {
        val equipmentId = context.sourceId
            ?: return EffectResult.error(state, "No source for attach equipment")

        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for attach equipment")

        var newState = state
        val unattachEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Detach from the current creature if already attached. Moving an Equipment onto a *new*
        // host makes it become unattached from the old one first (CR 701.3d), so this goes through
        // the shared chokepoint and reports a PermanentUnattachedEvent — that is how Stitcher's
        // Graft's "sacrifice that permanent" fires when you equip it away. Re-affirming the same
        // host is not an unattach, so it emits nothing.
        val currentAttachment = newState.getEntity(equipmentId)?.get<AttachedToComponent>()
        if (currentAttachment != null && currentAttachment.targetId != targetId) {
            val (detachedState, events) = ZoneMovementUtils.unattachEmittingEvent(newState, equipmentId)
            newState = detachedState
            unattachEvents += events
        } else if (currentAttachment != null) {
            newState = ZoneMovementUtils.cleanupReverseAttachmentLink(newState, equipmentId)
        }

        // Attach to new creature
        newState = newState.updateEntity(equipmentId) { container ->
            container.with(AttachedToComponent(targetId))
        }

        newState = newState.updateEntity(targetId) { container ->
            val existing = container.get<AttachmentsComponent>()
            val updatedIds = (existing?.attachedIds ?: emptyList()) + equipmentId
            container.with(AttachmentsComponent(updatedIds))
        }

        // CR 603.2e — emit a "becomes attached" event only when the equipment moved onto a *new*
        // host (not when re-affirming an existing attachment), so attachment triggers fire at the
        // right moment.
        val events = if (currentAttachment?.targetId != targetId) {
            val container = newState.getEntity(equipmentId)
            unattachEvents + PermanentAttachedEvent(
                attachmentId = equipmentId,
                attachmentName = container?.get<CardComponent>()?.name ?: "Equipment",
                attachedToId = targetId,
                controllerId = container?.get<ControllerComponent>()?.playerId ?: context.controllerId,
            )
        } else emptyList()

        return EffectResult.success(newState, events)
    }
}
