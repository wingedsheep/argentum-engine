package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Keeps opponents informed of cards whose identity became public *as they entered their
 * owner's hand*, and lets that knowledge expire the way it does on MTGO/Arena: a card a
 * player reveals into their hand, or returns to their hand from a public zone, stays
 * face-up to opponents until that player **plays a card with the same name** — at which
 * point the opponent can no longer tell the known copy from the one just played, so the
 * knowledge is forgotten.
 *
 * Visibility itself is the existing [RevealedToComponent] mechanism consulted by
 * [com.wingedsheep.engine.view.ClientStateTransformer]; this tracker only manages the
 * *lifecycle* of that component for cards sitting in the [Zone.HAND]:
 *
 *  - **Reveal into hand** — a [CardsRevealedEvent] for a card now in its owner's hand
 *    (CR 701.16: revealing shows the card to every player), and **return to hand from a
 *    public zone** — a [ZoneChangeEvent] into [Zone.HAND] from the battlefield, a
 *    graveyard, or the stack — mark the card known to the whole table.
 *  - **Playing a same-named card from hand** — a [SpellCastEvent] whose spell was cast
 *    from hand, or a land played from hand — forgets every still-known copy of that name
 *    in the player's hand.
 *  - **Leaving the hand** — a [ZoneChangeEvent] out of [Zone.HAND] to anywhere but a library
 *    drops that card's hand-knowledge, so it can't leak once the card is hidden again (e.g. cast
 *    face-down). A move *into* a library is left alone: `ZoneTransitionService` has already
 *    replaced the audience with whoever knows that library slot, and overwriting it here would
 *    blind the mover to a card they legitimately watched go in.
 *
 * It deliberately does **not** touch library/face-down reveals: a private tutor to hand
 * (a [Zone.LIBRARY] → [Zone.HAND] move with no reveal) stays hidden, returns from
 * [Zone.EXILE]/[Zone.SIDEBOARD] are left alone (a face-down-exiled card must not leak),
 * and only cards currently in a hand are ever marked or cleared.
 *
 * Pure and event-driven: it runs once over each action's emitted events from
 * [com.wingedsheep.engine.core.ActionProcessor], so it replays deterministically.
 */
object RevealedInHandTracker {

    /**
     * Zones whose contents are public knowledge, so a card returning from them to a hand
     * keeps being known to opponents. [Zone.EXILE]/[Zone.COMMAND] are excluded: cards can
     * sit face-down in exile, and marking those known on return would leak their identity.
     */
    private val PUBLIC_RETURN_SOURCES = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.STACK)

    /**
     * Apply the hand-reveal lifecycle for the events an action produced, returning the
     * (possibly updated) result. A no-op for results without events (errors, plain passes).
     */
    fun applyAfterAction(result: ExecutionResult): ExecutionResult {
        if (result.events.isEmpty()) return result
        var state = result.state
        for (event in result.events) {
            state = when (event) {
                is CardsRevealedEvent -> onCardsRevealed(state, event)
                is ZoneChangeEvent -> onZoneChange(state, event)
                is SpellCastEvent -> onSpellCast(state, event)
                else -> state
            }
        }
        return if (state === result.state) result else result.copy(state = state)
    }

    private fun onCardsRevealed(state: GameState, event: CardsRevealedEvent): GameState {
        val inHand = event.cardIds.filter { isInOwnersHand(state, it) }
        if (inHand.isEmpty()) return state
        return LibraryRevealUtils.markRevealed(state, inHand, state.turnOrder)
    }

    private fun onZoneChange(state: GameState, event: ZoneChangeEvent): GameState {
        // Returned to hand from a public zone → known to the whole table.
        if (event.toZone == Zone.HAND && event.fromZone in PUBLIC_RETURN_SOURCES) {
            return if (isInOwnersHand(state, event.entityId)) {
                LibraryRevealUtils.markRevealed(state, listOf(event.entityId), state.turnOrder)
            } else {
                state
            }
        }
        // Left the hand → forget this card's hand-knowledge so it can't leak once hidden.
        //
        // A library destination is the exception, and deliberately so: ZoneTransitionService has
        // already *replaced* the card's audience with whoever legitimately knows that library slot
        // (LibraryRevealUtils.placementAudience — nobody on a shuffle, the mover on a hand
        // put-back like Conch Horn, the table on a public move). Clearing here would wipe that
        // authoritative answer and blind the player to the card they just chose to put back.
        // Leaks are prevented by that replace, not by this clear.
        if (event.fromZone == Zone.HAND && event.toZone != Zone.LIBRARY) {
            var newState = LibraryRevealUtils.clearReveals(state, listOf(event.entityId))
            // A land enters the battlefield straight from hand (it is never cast), so the
            // land play itself is the "play a same-named card" trigger.
            if (event.toZone == Zone.BATTLEFIELD) {
                newState = forgetSameNamedInHand(newState, event.ownerId, event.entityName)
            }
            return newState
        }
        return state
    }

    private fun onSpellCast(state: GameState, event: SpellCastEvent): GameState {
        // The cast card is public on the stack regardless; dropping its hand-knowledge also
        // stops a bounced-then-cast-face-down card from leaking its real identity.
        var newState = LibraryRevealUtils.clearReveals(state, listOf(event.spellEntityId))
        val castFromZone = state.getEntity(event.spellEntityId)
            ?.get<SpellOnStackComponent>()?.castFromZone
        // Only a card played *from hand* makes the known copies of its name ambiguous;
        // casting the same name from the graveyard/exile leaves the hand copy identifiable.
        if (castFromZone == Zone.HAND) {
            newState = forgetSameNamedInHand(newState, event.casterId, event.cardName)
        }
        return newState
    }

    /** Forget every still-known card named [name] in [playerId]'s hand. */
    private fun forgetSameNamedInHand(state: GameState, playerId: EntityId, name: String): GameState {
        val sameNamed = state.getHand(playerId).filter { cardId ->
            val container = state.getEntity(cardId) ?: return@filter false
            container.get<RevealedToComponent>() != null &&
                container.get<CardComponent>()?.name == name
        }
        if (sameNamed.isEmpty()) return state
        return LibraryRevealUtils.clearReveals(state, sameNamed)
    }

    private fun isInOwnersHand(state: GameState, cardId: EntityId): Boolean {
        val ownerId = state.getEntity(cardId)?.get<CardComponent>()?.ownerId ?: return false
        return cardId in state.getHand(ownerId)
    }
}
