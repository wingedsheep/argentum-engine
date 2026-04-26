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

    /** Mark each card as revealed to the given players. */
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
}
