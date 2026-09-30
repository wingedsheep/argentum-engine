package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Modified (CR 700.9): the permanent has a counter on it, is equipped, or is enchanted by an Aura
 * *controlled by that permanent's controller* — an opponent's Aura on your creature doesn't count.
 * [controllerOf] resolves control through the caller's projection (Layer 2), so a stolen Aura or
 * creature is judged by who controls it now.
 */
fun isModified(state: GameState, entityId: EntityId, controllerOf: (EntityId) -> EntityId?): Boolean {
    val container = state.getEntity(entityId) ?: return false
    if (container.get<CountersComponent>()?.counters?.values?.any { it > 0 } == true) return true
    val attachments = container.get<AttachmentsComponent>() ?: return false
    return attachments.attachedIds.any { attachId ->
        val card = state.getEntity(attachId)?.get<CardComponent>()
        when {
            card?.typeLine?.isEquipment == true -> true
            card?.typeLine?.isAura == true -> {
                val hostController = controllerOf(entityId)
                hostController != null && controllerOf(attachId) == hostController
            }
            else -> false
        }
    }
}
