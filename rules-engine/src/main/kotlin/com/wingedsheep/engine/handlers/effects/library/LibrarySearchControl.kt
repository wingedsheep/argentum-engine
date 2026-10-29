package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.ControlsOpponentsLibrarySearchesComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Who takes over a library search — [com.wingedsheep.sdk.scripting.ControlOpponentsLibrarySearches]
 * (Opposition Agent) — and what happens to the cards that search finds. The sibling of
 * [LibrarySearchRestrictions], read at the same three pipeline seams:
 *
 *  - the search gather (`GatherCardsEffect.search`) asks [controllerOf] and, when someone takes the
 *    search over, publishes [PipelineState.searchControllerKey] beside the searched collection and
 *    adds the pool to [PipelineState.searchControlledPoolKey] for that player;
 *  - the pick over the searched collection goes to the controlling player ([decider]);
 *  - every move of a pool card exiles it instead and lets the controlling player play it
 *    ([grantPlay]) — the found cards, whichever step moves them.
 */
object LibrarySearchControl {

    /**
     * The player who controls [searcherId] while they search their own library, or null. Only a
     * search that looks through the searcher's own library ([libraryOwners]) is taken over. When
     * several permanents apply, the one that entered the battlefield most recently wins (Opposition
     * Agent ruling).
     */
    fun controllerOf(state: GameState, searcherId: EntityId, libraryOwners: Collection<EntityId>?): EntityId? {
        if (libraryOwners == null || searcherId !in libraryOwners) return null
        val projected = state.projectedState
        var best: EntityId? = null
        var bestTimestamp = Long.MIN_VALUE
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            if (!container.has<ControlsOpponentsLibrarySearchesComponent>()) continue
            if (projected.hasLostAllAbilities(entityId)) continue
            val controller = projected.getController(entityId) ?: continue
            if (!state.isOpponentOf(searcherId, controller)) continue
            val timestamp = container.get<BattlefieldEntryTimestampComponent>()?.timestamp ?: 0L
            // Ties (no entry stamp) fall back to battlefield order, which is entry order.
            if (best == null || timestamp >= bestTimestamp) {
                best = controller
                bestTimestamp = timestamp
            }
        }
        return best
    }

    /** Who answers a pick over [collection] that would otherwise go to [decider]. */
    fun decider(collections: Map<String, List<EntityId>>, collection: String, decider: EntityId): EntityId {
        val searcher = collections[PipelineState.searchedKey(collection)]?.firstOrNull() ?: return decider
        if (searcher != decider) return decider
        return collections[PipelineState.searchControllerKey(collection)]?.firstOrNull() ?: decider
    }

    /**
     * The pick over a controlled search's [collection] decides what was found: the cards of
     * [offered] left unpicked drop out of that player's pool, so no later step exiles them. A search
     * with no pick (gather, then move every match) keeps its whole pool. Empty when [collection]
     * isn't a controlled search.
     */
    fun narrowPool(
        collections: Map<String, List<EntityId>>,
        collection: String,
        offered: List<EntityId>,
        selected: List<EntityId>,
    ): Map<String, List<EntityId>> {
        val controller = collections[PipelineState.searchControllerKey(collection)]?.firstOrNull() ?: return emptyMap()
        val poolKey = PipelineState.searchControlledPoolKey(controller)
        val unpicked = offered.toSet() - selected.toSet()
        return mapOf(poolKey to collections[poolKey].orEmpty().filter { it !in unpicked })
    }

    /** The cards of [cards] a controlled search found, each with the player who controlled it. */
    fun foundCards(collections: Map<String, List<EntityId>>, cards: List<EntityId>): Map<EntityId, EntityId> {
        val pools = collections.filterKeys { it.startsWith(PipelineState.SEARCH_CONTROLLED_POOL_PREFIX) }
        if (pools.isEmpty()) return emptyMap()
        val found = LinkedHashMap<EntityId, EntityId>()
        for (card in cards) {
            for ((key, pool) in pools) {
                if (card in pool) {
                    found[card] = EntityId(key.removePrefix(PipelineState.SEARCH_CONTROLLED_POOL_PREFIX))
                    break
                }
            }
        }
        return found
    }

    /**
     * "You may play those cards for as long as they remain exiled, and you may spend mana as though
     * it were mana of any color to cast them" — a permission that never expires on its own; it stops
     * mattering once the card leaves exile.
     */
    fun grantPlay(state: GameState, cards: Collection<EntityId>, playerId: EntityId, sourceId: EntityId?): GameState {
        val exiled = cards.filter { state.logicalZone(it)?.zoneType == Zone.EXILE }
        if (exiled.isEmpty()) return state
        val (permissionId, withId) = state.newEntity()
        return withId.addMayPlayPermission(
            MayPlayPermission(
                id = permissionId,
                cardIds = exiled.toSet(),
                controllerId = playerId,
                sourceId = sourceId,
                withAnyManaColor = true,
                permanent = true,
                timestamp = state.timestamp,
            )
        )
    }
}
