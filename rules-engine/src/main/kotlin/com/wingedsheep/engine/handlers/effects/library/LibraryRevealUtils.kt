package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Helpers for tracking which library cards a player is allowed to see.
 *
 * Reveals are stored as [RevealedToComponent] on the card entity. They persist while
 * the card is in a hidden zone (library/hand) and are cleared on shuffle so a freshly
 * shuffled library is once again opaque to everyone.
 */
object LibraryRevealUtils {

    /**
     * Zones whose contents every player can see, so a card moved from one into a library at a
     * known position is known to the whole table — everyone watched which card went where.
     *
     * [Zone.EXILE] and [Zone.COMMAND] are excluded: a card can sit face down in exile, and a
     * face-down exile returning to a library must not leak. Mirrors the same exclusion in
     * [com.wingedsheep.engine.mechanics.RevealedInHandTracker].
     */
    private val PUBLIC_SOURCE_ZONES = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.STACK)

    /**
     * Who legitimately knows the identity of a card placed into a library at a *known* position.
     *
     * CR 401.2 stops a player looking through a library; it does not erase what they just watched
     * go in. CR 401.4 is the other half: when several cards go in at once the owner does not
     * reveal the order, so an onlooker who did not see them learns only that cards were placed.
     * Three cases, and this is the engine's single statement of them:
     *
     *  - **Placed at a random position** (shuffled, or an ordering the mover didn't choose) — nobody.
     *  - **Publicly revealed on the way in, or moved out of a public zone** ([PUBLIC_SOURCE_ZONES]:
     *    Time Ebb putting a creature on top of its owner's library) — the whole table.
     *  - **Otherwise** (out of a hand or another hidden zone: Conch Horn's put-back, Brainstorm) —
     *    only the player who made the move, who chose the card and saw where it landed.
     *
     * @param moverId the player performing the move; `null` when no player is responsible, which
     *   yields no knowledge rather than guessing at an audience.
     */
    fun placementAudience(
        fromZone: Zone?,
        publiclyRevealed: Boolean,
        moverId: EntityId?,
        allPlayers: List<EntityId>,
        knownPosition: Boolean,
    ): Set<EntityId> = when {
        !knownPosition -> emptySet()
        publiclyRevealed || fromZone in PUBLIC_SOURCE_ZONES -> allPlayers.toSet()
        moverId != null -> setOf(moverId)
        else -> emptySet()
    }

    /**
     * **Replace** each card's reveal audience with exactly [playerIds] — the authoritative write
     * for a library placement.
     *
     * Replacing, not merging, is the whole point: a card that was public knowledge in a hand
     * (revealed there, or returned to hand from the battlefield) becomes hidden again the moment
     * it is tucked into a library, and any player outside [playerIds] must lose it. An empty
     * audience strips the component outright. [markRevealed] merges instead, and is for reveals
     * that add to what a player already knows.
     */
    fun setPlacementKnowledge(
        state: GameState,
        cardIds: Collection<EntityId>,
        playerIds: Set<EntityId>,
    ): GameState {
        if (cardIds.isEmpty()) return state
        var newState = state
        for (cardId in cardIds) {
            val container = newState.getEntity(cardId) ?: continue
            val current = container.get<RevealedToComponent>()?.playerIds ?: emptySet()
            if (current == playerIds) continue
            newState = newState.updateEntity(cardId) { c ->
                if (playerIds.isEmpty()) c.without<RevealedToComponent>()
                else c.with(RevealedToComponent(playerIds))
            }
        }
        return newState
    }

    /** Mark each card as revealed to the given players, *in addition* to who already knows it. */
    fun markRevealed(
        state: GameState,
        cardIds: Collection<EntityId>,
        playerIds: Collection<EntityId>
    ): GameState {
        if (cardIds.isEmpty() || playerIds.isEmpty()) return state
        var newState = state
        for (cardId in cardIds) {
            newState = newState.updateEntity(cardId) { container ->
                val existing = container.get<RevealedToComponent>()
                val merged = if (existing == null) {
                    RevealedToComponent(playerIds.toSet())
                } else {
                    existing.copy(playerIds = existing.playerIds + playerIds)
                }
                container.with(merged)
            }
        }
        return newState
    }

    /** Strip [RevealedToComponent] from every card currently in [ownerId]'s library. */
    fun clearLibraryReveals(state: GameState, ownerId: EntityId): GameState {
        val library = state.getZone(ZoneKey(ownerId, Zone.LIBRARY))
        if (library.isEmpty()) return state
        var newState = state
        for (cardId in library) {
            val container = newState.getEntity(cardId) ?: continue
            if (container.get<RevealedToComponent>() != null) {
                newState = newState.updateEntity(cardId) { c -> c.without<RevealedToComponent>() }
            }
        }
        return newState
    }

    /**
     * Strip [RevealedToComponent] from a specific set of cards.
     *
     * Use this for **per-card** reveal opacity (e.g. random bottom-of-library placement,
     * where the player loses knowledge of *these* cards' positions but retains knowledge
     * of any other cards already revealed elsewhere in the library). For wholesale opacity
     * after a shuffle, use [clearLibraryReveals] instead.
     */
    fun clearReveals(state: GameState, cardIds: Collection<EntityId>): GameState {
        if (cardIds.isEmpty()) return state
        var newState = state
        for (cardId in cardIds) {
            val container = newState.getEntity(cardId) ?: continue
            if (container.get<RevealedToComponent>() != null) {
                newState = newState.updateEntity(cardId) { c -> c.without<RevealedToComponent>() }
            }
        }
        return newState
    }
}
