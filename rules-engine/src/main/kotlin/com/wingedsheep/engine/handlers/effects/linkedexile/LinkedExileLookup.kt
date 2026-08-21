package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Reading the cards in a permanent's linked-exile pile
 * ([LinkedExileComponent]) — the pile that `linkToSource = true` exiles write and that Mirrodin's
 * *Imprint* payoffs (CR 702.15) read back.
 *
 * The one rule every caller shares: **a linked id is only interesting while the card is still in
 * exile.** The component deliberately persists across the source's own zone change so
 * leaves-the-battlefield triggers can still read it, and it is never pruned when a card leaves
 * exile by some other route, so the membership check is not optional. Centralizing it here keeps
 * the several independent readers (predicate evaluation, dynamic amounts, state projection) from
 * each re-deriving it — and each getting it subtly differently.
 */
object LinkedExileLookup {

    /**
     * The ids in [sourceId]'s linked-exile pile that are still in an exile zone, in exile order
     * (oldest first). Empty when the source has no pile, or when every card in it has left exile.
     */
    fun exiledCards(state: GameState, sourceId: EntityId?): List<EntityId> {
        val linked = sourceId?.let { state.getEntity(it)?.get<LinkedExileComponent>() } ?: return emptyList()
        return linked.exiledIds.filter { isStillExiled(state, it) }
    }

    /**
     * The [index]-th card still exiled with [sourceId], or null when the pile is shorter than that.
     * Backs [com.wingedsheep.sdk.scripting.values.EntityReference.LinkedExiledCard], whose default
     * index 0 is "the exiled card" of every Imprint permanent (Imprint exiles exactly one).
     */
    fun exiledCard(state: GameState, sourceId: EntityId?, index: Int = 0): EntityId? =
        exiledCards(state, sourceId).getOrNull(index)

    /**
     * Whether [entityId] is currently in its owner's exile zone. Uses the owner-keyed zone directly
     * where the owner is known, falling back to a scan for the ownerless case (a token that ceased
     * to exist has neither, and correctly reports false).
     */
    private fun isStillExiled(state: GameState, entityId: EntityId): Boolean {
        val container = state.getEntity(entityId) ?: return false
        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: container.get<CardComponent>()?.ownerId
        if (ownerId != null) return entityId in state.getZone(ZoneKey(ownerId, Zone.EXILE))
        return state.zones.any { (zone, cards) -> zone.zoneType == Zone.EXILE && entityId in cards }
    }
}
