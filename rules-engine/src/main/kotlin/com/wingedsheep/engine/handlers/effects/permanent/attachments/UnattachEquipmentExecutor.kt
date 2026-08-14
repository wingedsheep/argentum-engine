package com.wingedsheep.engine.handlers.effects.permanent.attachments

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.sdk.scripting.effects.UnattachEquipmentEffect
import kotlin.reflect.KClass

/**
 * Executor for [UnattachEquipmentEffect].
 *
 * Unattaches an Aura/Equipment from its host without moving it to another zone (CR 701.3d): clears
 * the attachment's [AttachedToComponent] and drops it from the host's [AttachmentsComponent]. A
 * no-op when the target isn't currently attached, emitting no event. The inverse of the attach
 * executors — used for "unattach it" riders (Stolen Uniform).
 */
class UnattachEquipmentExecutor : EffectExecutor<UnattachEquipmentEffect> {

    override val effectType: KClass<UnattachEquipmentEffect> = UnattachEquipmentEffect::class

    override fun execute(
        state: GameState,
        effect: UnattachEquipmentEffect,
        context: EffectContext
    ): EffectResult {
        val attachmentId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state) // target gone / illegal — nothing to unattach

        // The shared chokepoint clears both ends of the link and reports the
        // PermanentUnattachedEvent; it no-ops when the target isn't attached to anything.
        val (newState, events) = ZoneMovementUtils.unattachEmittingEvent(state, attachmentId)
        return EffectResult.success(newState, events)
    }
}
