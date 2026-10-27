package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Whether a pipeline collection's *membership* depends on cards some player can't see (#3340).
 *
 * Every player sees whether a question is pending, so a step may skip a question, or answer it
 * itself, only on facts every player has. Two facts decide that for a card pool:
 *
 * - **Its cards.** A filter or restriction that reads a card in a hand or a library (or one looked
 *   at privately, or a face-down card) reads something hidden, so how many cards pass it is hidden
 *   too. [hiddenToSomeSeat] answers that from the cards themselves, through [Visibility].
 * - **How it was built.** A pool gathered by filtering a hidden zone ("search your library for a
 *   basic land card", "a creature card from your hand") has a hidden size even once every card in
 *   it is public, and an empty pool says nothing at all about its cards. The step that built it
 *   records that under [PipelineState.hiddenPoolKey]: a non-empty entry names the players whose
 *   hidden cards decided the pool. Steps deriving one pool from another carry the mark on.
 *
 * An unfiltered pool from a hidden zone — the top three cards of a library, a whole hand — is
 * *not* marked: its size is a public count. Only what reads its cards' characteristics is.
 */
object HiddenPools {

    /** Whether [collection]'s membership was decided by hidden cards. */
    fun isHidden(stored: Map<String, List<EntityId>>, collection: String): Boolean =
        stored[PipelineState.hiddenPoolKey(collection)].orEmpty().isNotEmpty()

    /** The players whose hidden cards decided [collection], empty when it is public. */
    fun deciders(stored: Map<String, List<EntityId>>, collection: String): List<EntityId> =
        stored[PipelineState.hiddenPoolKey(collection)].orEmpty()

    /**
     * The mark entries to publish for [collections], each decided by [deciders]. With no deciders,
     * an entry is written only to clear a stale mark a reused name still carries in [prior].
     */
    fun marks(
        collections: Iterable<String>,
        deciders: List<EntityId>,
        prior: Map<String, List<EntityId>>,
    ): Map<String, List<EntityId>> = collections.mapNotNull { name ->
        val key = PipelineState.hiddenPoolKey(name)
        when {
            deciders.isNotEmpty() -> key to deciders
            key in prior -> key to emptyList()
            else -> null
        }
    }.toMap()

    /**
     * The owners of the cards in [cards] whose identity some player may not know, in first-seen
     * order: a hand or library card not revealed to everyone, a card outside the game, a face-down
     * object. Empty when every player can see every card, counting the cards this pipeline has
     * revealed ([PipelineState.REVEALED_KEY] in [stored]). Without [visibility] (pure-data test
     * wiring) the zone alone decides.
     */
    fun hiddenOwners(
        state: GameState,
        cards: Iterable<EntityId>,
        visibility: Visibility?,
        stored: Map<String, List<EntityId>>,
    ): List<EntityId> {
        val revealed = stored[PipelineState.REVEALED_KEY].orEmpty().toSet()
        val owners = LinkedHashSet<EntityId>()
        for (id in cards) {
            if (id in revealed || !hiddenToSomeSeat(state, id, visibility)) continue
            owners += state.logicalZone(id)?.ownerId ?: continue
        }
        return owners.toList()
    }

    /** Whether some player may not know [cardId]'s identity. */
    fun hiddenToSomeSeat(state: GameState, cardId: EntityId, visibility: Visibility?): Boolean {
        val zone = state.logicalZone(cardId)?.zoneType ?: return false
        if (visibility == null) {
            return zone == Zone.HAND || zone == Zone.LIBRARY || zone == Zone.SIDEBOARD ||
                faceDownDisplayName(state, cardId) != null
        }
        return state.turnOrder.any { seat -> !visibility.isCardIdentityVisibleTo(state, zone, cardId, seat) }
    }
}
