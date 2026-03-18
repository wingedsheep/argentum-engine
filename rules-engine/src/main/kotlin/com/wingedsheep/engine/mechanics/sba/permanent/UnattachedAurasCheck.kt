package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.cleanupReverseAttachmentLink
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * 704.5n - An Aura attached to an illegal object/player or not attached goes to graveyard.
 * 704.5p - An Equipment or Fortification attached to an illegal permanent becomes unattached
 *          but remains on the battlefield.
 */
class UnattachedAurasCheck : StateBasedActionCheck {
    override val name = "704.5n/p Unattached Auras"
    override val order = SbaOrder.UNATTACHED_AURAS

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val isAura = cardComponent.typeLine.isAura
            val isEquipment = cardComponent.typeLine.isEquipment

            if (!isAura && !isEquipment) continue

            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo == null) {
                if (isAura) {
                    // Aura not attached to anything - goes to graveyard
                    val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                        newState, entityId, cardComponent
                    )
                    newState = result.newState
                    events.addAll(result.events)
                }
                // Equipment not attached to anything is fine - stays on battlefield
            } else {
                // Check if attached target still exists on battlefield
                if (attachedTo.targetId !in state.getBattlefield()) {
                    if (isAura) {
                        // Aura's target gone - goes to graveyard
                        val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                            newState, entityId, cardComponent,
                            lastKnownAttachedTo = attachedTo.targetId
                        )
                        newState = result.newState
                        events.addAll(result.events)
                    } else {
                        // Equipment's target gone - just detach, stays on battlefield
                        newState = cleanupReverseAttachmentLink(newState, entityId)
                        newState = newState.updateEntity(entityId) { c ->
                            c.without<AttachedToComponent>()
                        }
                    }
                }
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
