package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ForbidsLibrarySearchComponent
import com.wingedsheep.engine.state.components.player.CantSearchLibrariesComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.LibrarySearchProhibition
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Which libraries a player is forbidden to search (CR 701.23) — the one answer every search site
 * reads: the search gather (a forbidden library contributes no cards), the searched event (a
 * search that touched no permitted library is no search), and the "you may search" gate (a
 * forbidden search can't be chosen).
 *
 * Two sources forbid a search:
 *  - [CantSearchLibrariesComponent] on the player — the one-shot `CantSearchLibrariesEffect`
 *    (Shadow of Doubt): every library.
 *  - [ForbidsLibrarySearchComponent] on a battlefield permanent — the static
 *    [com.wingedsheep.sdk.scripting.PlayersCantSearchLibraries] (Stranglehold: every library;
 *    Ashiok, Dream Render: only the searcher's own library, and only when a spell or ability the
 *    searcher controls causes the search). A permanent that has lost its abilities forbids nothing.
 */
object LibrarySearchRestrictions {

    /**
     * The owners of the libraries [searcherId] can't search when a spell or ability controlled by
     * [causeControllerId] instructs them to — every player, only the searcher, or nobody.
     */
    fun forbiddenLibraryOwners(state: GameState, searcherId: EntityId, causeControllerId: EntityId): Set<EntityId> {
        val everyLibrary = state.turnOrder.toSet()
        if (state.getEntity(searcherId)?.has<CantSearchLibrariesComponent>() == true) return everyLibrary
        val projected = state.projectedState
        var ownLibrary = false
        for (entityId in state.getBattlefield()) {
            val marker = state.getEntity(entityId)?.get<ForbidsLibrarySearchComponent>() ?: continue
            if (projected.hasLostAllAbilities(entityId)) continue
            val sourceController = projected.getController(entityId) ?: continue
            for (prohibition in marker.prohibitions) {
                val covers = when (prohibition.affected) {
                    is Player.Each -> true
                    is Player.You -> searcherId == sourceController
                    is Player.EachOpponent -> state.isOpponentOf(searcherId, sourceController)
                    else -> false
                }
                if (!covers) continue
                when (prohibition.scope) {
                    LibrarySearchProhibition.ANY_LIBRARY -> return everyLibrary
                    LibrarySearchProhibition.OWN_LIBRARY_BY_OWN_EFFECTS ->
                        if (causeControllerId == searcherId) ownLibrary = true
                }
            }
        }
        return if (ownLibrary) setOf(searcherId) else emptySet()
    }

    /**
     * Whether [searcherId] is forbidden to make this search at all: every library it would look
     * through ([libraryOwners]) is forbidden. With [libraryOwners] unknown (null), only a ban on
     * every library counts.
     */
    fun isSearchForbidden(
        state: GameState,
        searcherId: EntityId,
        causeControllerId: EntityId,
        libraryOwners: Collection<EntityId>?
    ): Boolean {
        val forbidden = forbiddenLibraryOwners(state, searcherId, causeControllerId)
        if (forbidden.isEmpty()) return false
        if (libraryOwners == null) return forbidden.containsAll(state.turnOrder)
        return libraryOwners.isNotEmpty() && forbidden.containsAll(libraryOwners)
    }

    /**
     * The players whose libraries a search over [source] looks through — its player reference
     * fanned out by [resolvePlayers] — or null when it reads no library or the player can't be
     * resolved.
     */
    fun searchedLibraryOwners(source: CardSource, resolvePlayers: (Player) -> List<EntityId>?): List<EntityId>? =
        when (source) {
            is CardSource.FromZone -> source.player.takeIf { source.zone == Zone.LIBRARY }
            is CardSource.FromMultipleZones -> source.player.takeIf { Zone.LIBRARY in source.zones }
            else -> null
        }?.let(resolvePlayers)

    /**
     * Whether [source] reads nothing but libraries — "search your library", not "search your
     * graveyard and/or library", whose other zones a forbidden library leaves searchable.
     */
    fun readsOnlyLibraries(source: CardSource): Boolean = when (source) {
        is CardSource.FromZone -> source.zone == Zone.LIBRARY
        is CardSource.FromMultipleZones -> source.zones.isNotEmpty() && source.zones.all { it == Zone.LIBRARY }
        else -> false
    }
}
