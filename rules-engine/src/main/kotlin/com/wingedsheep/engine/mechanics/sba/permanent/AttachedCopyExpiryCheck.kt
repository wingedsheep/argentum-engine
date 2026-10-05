package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.handlers.effects.copy.expireCopyLayers
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.CopyHistoryComponent
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.engine.core.CopiableCharacteristicsChangedEvent
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
class AttachedCopyExpiryCheck(private val cardRegistry: CardRegistry) : StateBasedActionCheck {
    override val name = "611.2b Attached-Copy Expiry"
    override val order = SbaOrder.ATTACHED_COPY_EXPIRY

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        var changed = false
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val marker = container.get<CopyWhileAttachedComponent>() ?: continue

            fun attached(id: com.wingedsheep.sdk.model.EntityId?): Boolean = id != null &&
                id in state.getBattlefield() && state.getEntity(id)?.get<AttachedToComponent>()?.targetId == entityId
            val history = container.get<CopyHistoryComponent>()
            if (history != null) {
                if (history.layers.none { it.duration == Duration.WhileSourceAttachedToAffected && !attached(it.attachmentId) }) continue
            } else if (attached(marker.attachmentId)) continue
            newState = newState.updateEntity(entityId) { c ->
                c.expireCopyLayers(cardRegistry) {
                    it.duration == Duration.WhileSourceAttachedToAffected && !attached(it.attachmentId)
                }.let { updated ->
                    val next = updated.get<CopyHistoryComponent>()?.layers
                        ?.lastOrNull { it.duration == Duration.WhileSourceAttachedToAffected }?.attachmentId
                    if (next == null) updated.without<CopyWhileAttachedComponent>() else updated.with(CopyWhileAttachedComponent(next))
                }
            }
            changed = true
            events.add(CopiableCharacteristicsChangedEvent(entityId))
        }

        return if (changed) ExecutionResult.success(newState, events) else ExecutionResult.success(state)
    }
}
