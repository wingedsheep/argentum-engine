package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PairedComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Reads of the soulbond pairing link (CR 702.95b), shared by every consumer of
 * [com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair] and of
 * [com.wingedsheep.sdk.scripting.predicates.StatePredicate.IsPaired].
 *
 * The scope and the predicate resolve in a dozen places — layer projection, granted-ability
 * enumeration and its mirror in the activation handler, trigger gating, block restrictions, cost
 * modification — and each one only needs one of the three questions below. Keeping them here is what
 * stops those sites drifting apart the way near-clone scope branches otherwise do.
 *
 * **Reads are self-validating.** CR 702.95e is a continuous fact, not a game action: the instant a
 * half leaves the battlefield the two creatures *are* unpaired. `SoulbondPairingCheck` is what
 * removes the components and emits the event, but it only runs before a player gets priority — too
 * late for a creature that leaves and re-enters inside one resolution (Deadeye Navigator blinking its
 * own partner), whose enters-the-battlefield trigger would otherwise be evaluated against a stale
 * pairing and never fire. So every read below re-checks the link rather than trusting the component,
 * and the SBA becomes the tidy-up rather than the source of truth.
 *
 * The re-check here is deliberately projection-free — battlefield presence and symmetry only. The
 * other two CR 702.95e cases (a controller change, a half that stopped being a creature) need
 * projected state to see, so they stay with the SBA; neither can lose a trigger the way a zone change
 * can, because nothing enters the battlefield.
 */
object SoulbondPairing {

    /**
     * The creature [entityId] is currently paired with, or `null` if it is unpaired — including when
     * it still carries a [PairedComponent] whose other half has already left the battlefield or been
     * cleared (CR 702.95e; see the class note on why the component alone isn't the answer).
     */
    fun partnerOf(state: GameState, entityId: EntityId): EntityId? {
        val partnerId = state.getEntity(entityId)?.get<PairedComponent>()?.partnerId ?: return null
        if (partnerId !in state.getBattlefield()) return null
        if (state.getEntity(partnerId)?.get<PairedComponent>()?.partnerId != entityId) return null
        return partnerId
    }

    /** Is [entityId] soulbond-paired right now? Backs `StatePredicate.IsPaired`. */
    fun isPaired(state: GameState, entityId: EntityId): Boolean = partnerOf(state, entityId) != null

    /**
     * "Both creatures" of [sourceId]'s pair — the source and its partner — or the empty set while
     * the source is unpaired. An unpaired source affecting nothing is what makes a payoff static's
     * "as long as this creature is paired" clause self-enforcing.
     */
    fun pairOf(state: GameState, sourceId: EntityId): Set<EntityId> {
        val partnerId = partnerOf(state, sourceId) ?: return emptySet()
        return setOf(sourceId, partnerId)
    }

    /**
     * Is [entityId] one of the two creatures in [sourceId]'s pair? False whenever [sourceId] is
     * unpaired, including when `entityId == sourceId`.
     */
    fun isInPairOf(state: GameState, sourceId: EntityId, entityId: EntityId): Boolean =
        entityId in pairOf(state, sourceId)
}
