package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.HandObjectRestriction
import com.wingedsheep.engine.state.components.identity.HandObjectRestrictionKind
import com.wingedsheep.engine.state.components.identity.HandObjectRestrictionsComponent
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * Card-scoped restrictions on one object in a hand — a card-targeted
 * [com.wingedsheep.sdk.scripting.effects.CantPlayCardsFromHandEffect] ("… can't play it") and
 * [com.wingedsheep.sdk.scripting.effects.RevealCardInHandEffect] ("plays with that card revealed in
 * their hand"), both stored on the card as a [HandObjectRestrictionsComponent].
 *
 * The one place that applies, reads and expires them:
 *  - **apply** — the executors and the inline replacement rider ([com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect])
 *    both come here, so Firestorm Phoenix's "return it to its owner's hand instead" rider and a
 *    resolving spell behave the same.
 *  - **read** — [isCantBePlayed] gates the cast validator, the land-play handler and legal-action
 *    enumeration; [isPinnedRevealed] keeps [RevealedInHandTracker] from forgetting the card early.
 *  - **expire** — at the post-untap hook of the expiry player's turn and at cleanup; and when the
 *    card leaves the hand (CR 400.7: it becomes a new object) via [dropStale].
 */
object HandObjectRestrictions {

    /**
     * Pin [kind] to [cardId] for [duration], if the card is in its owner's hand. Returns null when
     * [duration] has no hand-object meaning (the caller reports it); a card that isn't in a hand is
     * a successful no-op — the object the effect named is gone.
     *
     * @param effectControllerId the effect's "you", for [Duration.UntilYourNextTurn].
     */
    fun apply(
        state: GameState,
        cardId: EntityId,
        kind: HandObjectRestrictionKind,
        duration: Duration,
        effectControllerId: EntityId?,
        cardRegistry: CardRegistry? = null
    ): Pair<GameState, List<GameEvent>>? {
        val card = state.getEntity(cardId)?.get<CardComponent>() ?: return state to emptyList()
        val ownerId = card.ownerId ?: return state to emptyList()
        val (removeOn, expiresFor) = when (duration) {
            Duration.EndOfTurn -> PlayerEffectRemoval.EndOfTurn to null
            Duration.Permanent -> PlayerEffectRemoval.Permanent to null
            Duration.UntilAffectedOwnersNextTurn -> PlayerEffectRemoval.UntilYourNextTurn to ownerId
            Duration.UntilYourNextTurn ->
                PlayerEffectRemoval.UntilYourNextTurn to (effectControllerId ?: return null)
            else -> return null
        }
        if (cardId !in state.getHand(ownerId)) return state to emptyList()
        val generation = state.objectRef(cardId)?.generation ?: return state to emptyList()

        val entry = HandObjectRestriction(kind, generation, removeOn, expiresFor)
        var newState = state.updateEntity(cardId) { c ->
            val current = c.get<HandObjectRestrictionsComponent>()?.restrictions
                ?.filter { it.objectGeneration == generation }
                .orEmpty()
            c.with(HandObjectRestrictionsComponent(current + entry))
        }
        val events = mutableListOf<GameEvent>()
        if (kind == HandObjectRestrictionKind.REVEALED) {
            newState = LibraryRevealUtils.markRevealed(newState, listOf(cardId), newState.turnOrder)
            events += CardsRevealedEvent(
                revealingPlayerId = ownerId,
                cardIds = listOf(cardId),
                cardNames = listOf(card.name),
                imageUris = listOf(cardRegistry?.getCard(card.cardDefinitionId)?.metadata?.imageUri),
                source = card.name,
                // The owner already knows their own card; the reveal is for everyone else.
                revealToSelf = false
            )
        }
        return newState to events
    }

    /** Whether an active "can't play it" restriction covers [cardId] as the object it is now. */
    fun isCantBePlayed(state: GameState, cardId: EntityId): Boolean =
        isActive(state, cardId, HandObjectRestrictionKind.CANT_BE_PLAYED)

    /** Whether [cardId] is being played with revealed, so its hand knowledge must not lapse. */
    fun isPinnedRevealed(state: GameState, cardId: EntityId): Boolean =
        isActive(state, cardId, HandObjectRestrictionKind.REVEALED)

    private fun isActive(state: GameState, cardId: EntityId, kind: HandObjectRestrictionKind): Boolean {
        val restrictions = state.getEntity(cardId)?.get<HandObjectRestrictionsComponent>()?.restrictions
            ?: return false
        val generation = state.objectRef(cardId)?.generation ?: return false
        return restrictions.any { it.kind == kind && it.objectGeneration == generation }
    }

    /**
     * Drop the restrictions on [cardId] that named an earlier object — it left the hand and is a
     * new object (CR 400.7). Keyed on the object generation rather than wiping the component, so a
     * restriction already pinned to the card's *current* hand object survives.
     */
    fun dropStale(state: GameState, cardId: EntityId): GameState {
        val component = state.getEntity(cardId)?.get<HandObjectRestrictionsComponent>() ?: return state
        val generation = state.objectRef(cardId)?.generation
        val remaining = component.restrictions.filter { it.objectGeneration == generation }
        if (remaining.size == component.restrictions.size) return state
        return state.updateEntity(cardId) {
            if (remaining.isEmpty()) it.without<HandObjectRestrictionsComponent>()
            else it.with(HandObjectRestrictionsComponent(remaining))
        }
    }

    /**
     * Expire the "until <player>'s next turn" restrictions whose expiry player is on the turn
     * that is starting ([activeTeam]); called from the same post-untap hook as every other
     * `UntilYourNextTurn` lifetime.
     */
    fun expireAtTurnStart(state: GameState, activeTeam: Set<EntityId>): GameState =
        expireWhere(state) {
            it.removeOn == PlayerEffectRemoval.UntilYourNextTurn && it.expiresForPlayerId in activeTeam
        }

    /** Expire the "this turn" restrictions; called from the cleanup step. */
    fun expireAtEndOfTurn(state: GameState): GameState =
        expireWhere(state) { it.removeOn == PlayerEffectRemoval.EndOfTurn }

    private fun expireWhere(state: GameState, expired: (HandObjectRestriction) -> Boolean): GameState {
        var newState = state
        for (playerId in state.turnOrder) {
            for (cardId in state.getHand(playerId)) {
                val component = newState.getEntity(cardId)?.get<HandObjectRestrictionsComponent>() ?: continue
                val generation = newState.objectRef(cardId)?.generation
                val remaining = component.restrictions.filter { it.objectGeneration == generation && !expired(it) }
                if (remaining.size == component.restrictions.size) continue
                newState = newState.updateEntity(cardId) {
                    if (remaining.isEmpty()) it.without<HandObjectRestrictionsComponent>()
                    else it.with(HandObjectRestrictionsComponent(remaining))
                }
            }
        }
        return newState
    }
}
