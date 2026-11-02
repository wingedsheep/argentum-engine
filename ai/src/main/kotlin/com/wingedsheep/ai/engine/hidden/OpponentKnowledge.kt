package com.wingedsheep.ai.engine.hidden

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * What one engine AI seat knows about its opponents' decks, beyond the board.
 *
 * In live games the server hands the AI every opponent's **registered decklist** — a deliberate
 * design decision: the AI may know the full list, the way a player who has scouted an open decklist
 * does. It never learns hand contents or library order; [Determinizer] samples those from the list
 * ([OpponentModel.KnownDecklist]), and [unseenCards] reads off what of the list has not shown up yet.
 * This is AI-internal knowledge on the server: nothing here is ever sent to a client.
 *
 * Decklists arrive after the AI is built (a seat is wired before every opponent has registered a
 * deck), so the map is mutable. Each write publishes a fresh immutable map, so a reader on another
 * thread sees one consistent snapshot.
 */
class OpponentKnowledge(
    private val cardRegistry: CardRegistry,
    initial: Map<EntityId, OpponentModel> = emptyMap(),
) {
    @Volatile
    private var current: Map<EntityId, OpponentModel> = initial

    /** Per-opponent sampling models for [Determinizer.sampleForSearch]. */
    val models: Map<EntityId, OpponentModel> get() = current

    /** Record [opponentId]'s registered decklist (card name → copies). */
    fun learnDecklist(opponentId: EntityId, cards: Map<String, Int>) {
        current = current + (opponentId to OpponentModel.KnownDecklist(cards.filterValues { it > 0 }))
    }

    fun knowsDecklistOf(opponentId: EntityId): Boolean = current[opponentId] is OpponentModel.KnownDecklist

    private val visibility by lazy {
        Visibility(cardRegistry, conditionEvaluator = PredicateEvaluator(cardRegistry).conditions)
    }

    @Volatile
    private var cache: CachedUnseen? = null

    private class CachedUnseen(
        val state: GameState,
        val viewerId: EntityId,
        val opponentId: EntityId,
        val unseen: Map<String, Int>?,
    )

    /**
     * The cards of [opponentId]'s decklist that [viewerId] has not seen this game: the list minus
     * every card of theirs whose identity the viewer can see — on the battlefield, in a graveyard,
     * in exile, on the stack, and any hand or library card revealed to the viewer. What is left is
     * in their hand or library (or was never drawn). Null when no decklist is known.
     */
    fun unseenCards(state: GameState, viewerId: EntityId, opponentId: EntityId): Map<String, Int>? {
        val list = (current[opponentId] as? OpponentModel.KnownDecklist)?.cards ?: return null
        cache?.let { if (it.state === state && it.viewerId == viewerId && it.opponentId == opponentId) return it.unseen }

        val hidden = HashSet<EntityId>()
        for (zone in HIDDEN_ZONES) {
            val key = ZoneKey(opponentId, zone)
            for (id in state.getZone(key)) {
                if (!visibility.isCardIdentityVisibleTo(state, key, id, viewerId)) hidden.add(id)
            }
        }
        val remaining = list.toMutableMap()
        for ((id, container) in state.entities) {
            if (id in hidden || container.has<TokenComponent>()) continue
            if (container.get<OwnerComponent>()?.playerId != opponentId) continue
            val name = container.get<CardComponent>()?.name ?: continue
            remaining.computeIfPresent(name) { _, n -> (n - 1).takeIf { it > 0 } }
        }
        cache = CachedUnseen(state, viewerId, opponentId, remaining)
        return remaining
    }

    private companion object {
        val HIDDEN_ZONES = listOf(Zone.HAND, Zone.LIBRARY, Zone.SIDEBOARD)
    }
}
