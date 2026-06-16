package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.CopyWhileAttachedComponent

/**
 * CR 611.2b — a "becomes a copy of … for as long as [an attachment] remains attached to it" copy
 * (Assimilation Aegis) ends the moment the tracked attachment is no longer attached to the copied
 * permanent, and does not restart.
 *
 * Reverts any permanent carrying a [CopyWhileAttachedComponent] whose tracked attachment
 * ([CopyWhileAttachedComponent.attachmentId]) is no longer on the battlefield or is no longer
 * attached to that permanent (the Equipment detached, moved to another creature, or left). The
 * revert restores the pre-copy [CopyOfComponent.originalCardComponent] snapshot and drops both the
 * `CopyOfComponent` and the marker — mirroring the end-of-turn copy revert, but keyed to attachment
 * rather than the cleanup step.
 */
class AttachedCopyExpiryCheck : StateBasedActionCheck {
    override val name = "611.2b Attached-Copy Expiry"
    override val order = SbaOrder.ATTACHED_COPY_EXPIRY

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        var changed = false

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val marker = container.get<CopyWhileAttachedComponent>() ?: continue

            // Still attached to this very permanent? Then the copy persists.
            val attachment = state.getEntity(marker.attachmentId)
            val stillAttached = marker.attachmentId in state.getBattlefield() &&
                attachment?.get<AttachedToComponent>()?.targetId == entityId
            if (stillAttached) continue

            // Revert to the pre-copy identity (CR 611.2b — one-way: drop the marker so a later
            // re-attach can't resurrect this copy).
            val originalCard = container.get<CopyOfComponent>()?.originalCardComponent
            newState = newState.updateEntity(entityId) { c ->
                var reverted = c.without<CopyWhileAttachedComponent>()
                if (originalCard != null) {
                    reverted = reverted.with(originalCard).without<CopyOfComponent>()
                }
                reverted
            }
            changed = true
        }

        return if (changed) ExecutionResult.success(newState) else ExecutionResult.success(state)
    }
}
