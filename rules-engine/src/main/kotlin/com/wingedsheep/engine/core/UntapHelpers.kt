package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

/**
 * Apply Rule 122.1d: a stun counter replaces "becoming untapped" by removing
 * itself instead. Used for both the natural untap step and explicit "untap
 * target permanent" effects.
 *
 * - If [entityId] is tapped and has a stun counter: removes one stun counter,
 *   leaves the permanent tapped, emits no event.
 * - If [entityId] is tapped and has no stun counter: untaps it and emits
 *   [UntappedEvent].
 * - If [entityId] is already untapped: no-op (the replacement only fires on a
 *   permanent that *would* become untapped — already-untapped permanents are
 *   not eligible).
 *
 * @return the updated state plus an optional [UntappedEvent] (null when the
 *   permanent stayed tapped or wasn't tapped to begin with).
 */
fun untapOrConsumeStun(state: GameState, entityId: EntityId): Pair<GameState, UntappedEvent?> {
    val container = state.getEntity(entityId) ?: return state to null
    if (!container.has<TappedComponent>()) return state to null

    val stunCounters = container.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
    if (stunCounters > 0) {
        val newState = state.updateEntity(entityId) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withRemoved(CounterType.STUN, 1))
        }
        return newState to null
    }

    val cardName = container.get<CardComponent>()?.name ?: "Permanent"
    val newState = state.updateEntity(entityId) { it.without<TappedComponent>() }
    return newState to UntappedEvent(entityId, cardName)
}
