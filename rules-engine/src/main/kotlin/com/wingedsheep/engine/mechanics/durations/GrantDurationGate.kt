package com.wingedsheep.engine.mechanics.durations

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.EncodedOnComponent
import com.wingedsheep.engine.event.GrantedTriggeredAbility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

/**
 * The "for as long as …" gates ([Duration] CR 611.2b) as they apply to a **grant** — an ability
 * handed to a permanent and parked in one of the `granted*Abilities` stores, which has no
 * floating-effect representation for [com.wingedsheep.engine.mechanics.layers.StateProjector] to
 * gate per projection frame.
 *
 * A grant needs the same two halves a continuous effect gets:
 *
 *  - a **per-read gate** — the reader (trigger lookup, legal-action enumeration) must not see a
 *    grant whose condition is false *right now*, even if no state-based action has run since the
 *    condition failed. Removing a mannequin counter mid-resolution has to strip the drawback
 *    before the next spell can target the creature, exactly as `StateProjector` drops a floating
 *    effect the moment its counter leaves;
 *  - a **one-way latch** — [com.wingedsheep.engine.mechanics.sba.permanent.EndedDurationExpiryCheck]
 *    physically removes the grant once its condition has failed, so re-adding the counter (or
 *    re-tapping the permanent) cannot resurrect it.
 *
 * Both halves ask this object the same question, which is the point: a gate that disagreed with
 * its own latch would let a grant flicker back on for one read.
 *
 * Every duration this doesn't know about — `EndOfTurn`, `Permanent`, the turn-keyed windows —
 * answers `true`, so callers can apply [holds] unconditionally.
 */
object GrantDurationGate {

    /** Whether the grant's "for as long as …" condition still holds. */
    fun holds(
        state: GameState,
        entityId: EntityId,
        sourceId: EntityId?,
        duration: Duration
    ): Boolean = sourceGateHolds(state, duration, sourceId, entityId) &&
        !affectedGateFails(state, entityId, duration)

    /**
     * Whether a granted triggered ability is live right now: its duration gate [holds], and — when
     * the grant recorded its layer-6 timestamp — no later "loses all abilities" effect has stripped
     * it (CR 613.7). Unlike the duration, the second half is not latched: the ability comes back
     * when the removal ends, as an ability-adding effect does once a later removal stops applying.
     */
    fun triggeredGrantLive(state: GameState, grant: GrantedTriggeredAbility): Boolean =
        holds(state, grant.entityId, grant.sourceId, grant.duration) &&
            (grant.timestamp == null ||
                !state.projectedState.lostAllAbilitiesAfter(grant.entityId, grant.timestamp))

    /**
     * Whether a *source-keyed* "for as long as …" gate still holds for the grant/effect made by
     * [sourceId] on [affectedId]. Returns `true` for every other duration, so callers can apply it
     * unconditionally.
     *
     * Depends only on the source's zone, tapped state, and attachment — no projection — which is
     * what lets the granted-ability path (which has no
     * [com.wingedsheep.engine.mechanics.layers.ProjectedState] at hand) share it with the
     * floating-effect path. A missing [sourceId] means there is no source on the battlefield, so
     * the gate is closed.
     */
    fun sourceGateHolds(
        state: GameState,
        duration: Duration,
        sourceId: EntityId?,
        affectedId: EntityId
    ): Boolean = when (duration) {
        // "for as long as this permanent remains on the battlefield" (Kitesail Larcenist).
        is Duration.WhileSourceOnBattlefield ->
            sourceId != null && state.getBattlefield().contains(sourceId)

        // "for as long as this creature remains tapped" (Old Man of the Sea).
        is Duration.WhileSourceTapped -> sourceTapped(state, sourceId)

        // "for as long as [the source Aura/Equipment] remains attached to it" — the source leaving
        // the battlefield, becoming unattached, or moving to a different host all end it.
        Duration.WhileSourceAttachedToAffected ->
            sourceId != null && state.getBattlefield().contains(sourceId) &&
                state.getEntity(sourceId)?.get<AttachedToComponent>()?.targetId == affectedId

        // The mirror: "until it becomes unattached from that creature" (reconfigure, CR 702.151b) —
        // the affected attachment must still be attached to the source host, which must still be
        // on the battlefield.
        Duration.WhileAffectedAttachedToSource ->
            sourceId != null && state.getBattlefield().contains(sourceId) &&
                state.getEntity(affectedId)?.get<AttachedToComponent>()?.targetId == sourceId

        // Cipher (CR 702.99c): the source card is still the same object in exile, still encoded on
        // the affected creature, which is still on the battlefield. (Its grants are dropped when it
        // re-enters as a new object; until then this keeps a departed creature's grant inert.)
        Duration.WhileSourceEncodedOnAffected -> sourceId != null &&
            state.getBattlefield().contains(affectedId) &&
            state.logicalZone(sourceId)?.zoneType == Zone.EXILE &&
            state.getEntity(sourceId)?.get<EncodedOnComponent>()?.let {
                it.creatureId == affectedId && state.isCurrentObject(it.cardObject)
            } == true

        else -> true
    }

    /**
     * True when [duration] is an affected-object-keyed "for as long as …" duration whose
     * condition no longer holds for [entityId]. False for every other duration.
     */
    fun affectedGateFails(
        state: GameState,
        entityId: EntityId,
        duration: Duration
    ): Boolean = when (duration) {
        is Duration.WhileAffectedHasCounter -> {
            if (!state.getBattlefield().contains(entityId)) true
            else {
                (state.getEntity(entityId)?.get<CountersComponent>()?.getCount(duration.counterType) ?: 0) <= 0
            }
        }

        Duration.WhileAffectedTapped ->
            !state.getBattlefield().contains(entityId) ||
                state.getEntity(entityId)?.has<TappedComponent>() != true

        else -> false
    }

    fun sourceTapped(state: GameState, sourceId: EntityId?): Boolean =
        sourceId != null && state.getBattlefield().contains(sourceId) &&
            state.getEntity(sourceId)?.has<TappedComponent>() == true
}
