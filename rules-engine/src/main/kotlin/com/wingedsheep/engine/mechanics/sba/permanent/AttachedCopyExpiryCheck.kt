package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.handlers.effects.copy.expireCopyLayers
import com.wingedsheep.engine.handlers.effects.permanent.types.becomeCopyInPlace
import com.wingedsheep.engine.handlers.effects.permanent.types.isAttachmentKeyed
import com.wingedsheep.engine.state.components.identity.CopyIdentityLayer
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
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
 *
 * A [Duration.WhileSourceAttached] copy is an Aura's own "enchanted creature is a copy of …" static
 * (Metamorphic Alteration): it ends on the old host the same way, but while its source is still on
 * the battlefield and attached to another permanent, the same locked-in copiable values move onto
 * that new host (CR 611.3a — a static ability applies to whatever it currently describes).
 */
class AttachedCopyExpiryCheck(private val cardRegistry: CardRegistry) : StateBasedActionCheck {
    override val name = "611.2b Attached-Copy Expiry"
    override val order = SbaOrder.ATTACHED_COPY_EXPIRY

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        var changed = false
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Copies that follow their source to its new host, applied once every stale host has reverted.
        val moved = mutableListOf<CopyIdentityLayer>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val marker = container.get<CopyWhileAttachedComponent>() ?: continue

            fun attached(id: com.wingedsheep.sdk.model.EntityId?): Boolean = id != null &&
                id in state.getBattlefield() && state.getEntity(id)?.get<AttachedToComponent>()?.targetId == entityId
            val history = container.get<CopyHistoryComponent>()
            if (history != null) {
                if (history.layers.none { it.duration.isAttachmentKeyed() && !attached(it.attachmentId) }) continue
                history.layers.filterTo(moved) { it.duration == Duration.WhileSourceAttached && !attached(it.attachmentId) }
            } else if (attached(marker.attachmentId)) continue
            newState = newState.updateEntity(entityId) { c ->
                c.expireCopyLayers(cardRegistry) {
                    it.duration.isAttachmentKeyed() && !attached(it.attachmentId)
                }.let { updated ->
                    val next = updated.get<CopyHistoryComponent>()?.layers
                        ?.lastOrNull { it.duration.isAttachmentKeyed() }?.attachmentId
                    if (next == null) updated.without<CopyWhileAttachedComponent>() else updated.with(CopyWhileAttachedComponent(next))
                }
            }
            changed = true
            events.add(CopiableCharacteristicsChangedEvent(entityId))
        }

        for (layer in moved) {
            val attachmentId = layer.attachmentId ?: continue
            if (attachmentId !in newState.getBattlefield()) continue
            val hostId = newState.getEntity(attachmentId)?.get<AttachedToComponent>()?.targetId ?: continue
            if (hostId !in newState.getBattlefield()) continue
            val controllerId = layer.controllerId ?: newState.projectedState.getController(attachmentId) ?: continue
            newState = becomeCopyInPlace(
                newState, hostId, layer.card, CopyExceptions.None, Duration.WhileSourceAttached,
                controllerId, cardRegistry, attachedSourceId = attachmentId,
            ) ?: continue
            events.add(CopiableCharacteristicsChangedEvent(hostId))
        }

        return if (changed) ExecutionResult.success(newState, events) else ExecutionResult.success(state)
    }
}
