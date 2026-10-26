package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Linked-exile reads for live sources and resolving abilities. Live-source queries use the
 * component index; an ability from a departed battlefield visit reads that visit's retained pile.
 * Zone removal invalidates links, and membership checks also exclude vanished tokens.
 */
object LinkedExileLookup {

    /** Read the original source visit, even after that entity has returned or ceased to exist. */
    fun exiledCards(state: GameState, context: EffectContext): List<EntityId> {
        val sourceId = context.sourceId ?: return emptyList()
        val timestamp = context.sourceBattlefieldTimestamp
        val current = state.getEntity(sourceId)
            ?.get<BattlefieldEntryTimestampComponent>()?.timestamp
        if (timestamp != null && timestamp != current) {
            return state.departedLinkedExile[timestamp].orEmpty().filter { isStillExiled(state, it) }
        }
        return exiledCards(state, sourceId)
    }

    /** Append to the resolving source visit, which need no longer be on the battlefield. */
    fun link(state: GameState, context: EffectContext, cards: List<EntityId>): GameState {
        val sourceId = context.sourceId ?: return state
        val timestamp = context.sourceBattlefieldTimestamp
        val current = state.getEntity(sourceId)
            ?.get<BattlefieldEntryTimestampComponent>()?.timestamp
        if (timestamp != null && timestamp != current) {
            return state.copy(departedLinkedExile = state.departedLinkedExile +
                (timestamp to (state.departedLinkedExile[timestamp].orEmpty() + cards)))
        }
        val existing = state.getEntity(sourceId)?.get<LinkedExileComponent>()?.exiledIds.orEmpty()
        return state.updateEntity(sourceId) { it.with(LinkedExileComponent(existing + cards)) }
    }

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
     * Backs [com.wingedsheep.sdk.scripting.targets.EffectTarget.LinkedExiledCard], whose default
     * index 0 is "the exiled card" of every Imprint permanent (Imprint exiles exactly one).
     */
    fun exiledCard(state: GameState, sourceId: EntityId?, index: Int = 0): EntityId? =
        exiledCards(state, sourceId).getOrNull(index)

    /**
     * The pile an ability of [sourceId] means by "cards exiled with ~". For a permanent that is its
     * own pile. For an emblem whose text names the object that created it — "You may play cards
     * exiled with Tibalt, Cosmic Impostor" — it is that creator's battlefield visit
     * ([EmblemSourceComponent.creatorEntryTimestamp]): the live pile while that visit lasts, then the
     * retained pile of the departed visit, so the cards stay covered after Tibalt leaves (and the
     * cards a −8 that killed him exiled are added there) while a later Tibalt — a new object, CR
     * 400.7 — is a different visit.
     */
    fun exiledWith(state: GameState, sourceId: EntityId): List<EntityId> {
        val container = state.getEntity(sourceId) ?: return emptyList()
        val own = container.get<LinkedExileComponent>()?.exiledIds.orEmpty()
        val emblem = container.get<EmblemSourceComponent>() ?: return own
        val creatorId = emblem.creatorId ?: return own
        val visit = emblem.creatorEntryTimestamp ?: return own
        val creator = state.getEntity(creatorId)
        val creatorPile = if (creator?.get<BattlefieldEntryTimestampComponent>()?.timestamp == visit) {
            creator.get<LinkedExileComponent>()?.exiledIds.orEmpty()
        } else {
            state.departedLinkedExile[visit].orEmpty()
        }
        return own + creatorPile
    }

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
