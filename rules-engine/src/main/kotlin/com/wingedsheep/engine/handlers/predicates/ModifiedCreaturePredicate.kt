package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

fun isModified(state: GameState, entityId: EntityId): Boolean {
    val container = state.getEntity(entityId) ?: return false
    val attachments = container.get<AttachmentsComponent>()
    val hasEquipmentOrAura = attachments != null && attachments.attachedIds.any { attachId ->
        val card = state.getEntity(attachId)?.get<CardComponent>()
        card?.typeLine?.isEquipment == true || card?.typeLine?.isAura == true
    }
    val hasCounter = container.get<CountersComponent>()?.counters?.values?.any { it > 0 } == true
    return hasEquipmentOrAura || hasCounter
}
