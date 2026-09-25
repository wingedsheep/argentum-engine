package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import kotlin.reflect.KClass

/**
 * Executor for AttachEquipmentEffect.
 * Attaches an equipment to a target creature, detaching from the previous creature if any.
 *
 * Moving an Equipment onto a *new* host makes it become unattached from the old one first
 * (CR 701.3d), so this reports a PermanentUnattachedEvent — that is how Stitcher's Graft's
 * "sacrifice that permanent" fires when you equip it away — followed by a PermanentAttachedEvent
 * (CR 603.2e). Re-affirming the same host emits nothing. See [AttachmentMover.attach].
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

        val (newState, events) = AttachmentMover.attach(state, equipmentId, targetId, context.controllerId)
        return EffectResult.success(newState, events)
    }
}
