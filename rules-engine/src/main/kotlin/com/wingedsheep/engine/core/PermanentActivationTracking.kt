package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.PermanentAbilitiesActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.projectedTypeLine
import com.wingedsheep.sdk.model.EntityId

/**
 * The lightweight [EntitySnapshot] of a permanent whose ability is being activated, as
 * [PermanentAbilitiesActivatedThisTurnComponent] stores it: identity, controller, projected type
 * line, sub/supertypes, keywords and token-ness — every field `PredicateEvaluator.matchesSnapshot`
 * answers, and its caller invariant requires (`wasToken` and `keywords` populated), and nothing
 * else, since auto-pay records one of these for every source it taps. Null when [sourceId] is not
 * on the battlefield.
 */
fun activationSourceSnapshot(state: GameState, sourceId: EntityId): EntitySnapshot? {
    if (sourceId !in state.getBattlefield()) return null
    val projected = state.projectedState
    return EntitySnapshot(
        entityId = sourceId,
        subtypes = projected.getSubtypes(sourceId),
        supertypes = projected.getSupertypes(sourceId),
        controllerId = projected.getController(sourceId),
        keywords = projected.getKeywords(sourceId),
        typeLine = projectedTypeLine(state, sourceId),
        wasToken = state.getEntity(sourceId)?.has<TokenComponent>() == true,
    )
}

/**
 * Record on [playerId] that they activated an ability of [snapshot]'s permanent this turn — see
 * [PermanentAbilitiesActivatedThisTurnComponent]. A null snapshot (the source isn't a permanent)
 * and an already-recorded identical snapshot are no-ops.
 */
fun recordPermanentActivation(state: GameState, playerId: EntityId, snapshot: EntitySnapshot?): GameState {
    if (snapshot == null) return state
    val tracker = state.getEntity(playerId)?.get<PermanentAbilitiesActivatedThisTurnComponent>()
        ?: PermanentAbilitiesActivatedThisTurnComponent()
    if (snapshot in tracker.sources) return state
    return state.updateEntity(playerId) { it.with(tracker.copy(sources = tracker.sources + snapshot)) }
}

/** [recordPermanentActivation] for a source that is still on the battlefield in [state]. */
fun recordPermanentActivation(state: GameState, playerId: EntityId, sourceId: EntityId): GameState =
    recordPermanentActivation(state, playerId, activationSourceSnapshot(state, sourceId))
