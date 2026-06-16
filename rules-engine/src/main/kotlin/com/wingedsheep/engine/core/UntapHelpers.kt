package com.wingedsheep.engine.core

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

/**
 * Apply the untap-step untap replacements to a single permanent that *would*
 * become untapped:
 *
 * - **Stun counters** (Rule 122.1d): if the permanent has a stun counter, remove
 *   one stun counter and leave it tapped (no [UntappedEvent]). This applies to
 *   both the natural untap step and explicit "untap target permanent" effects.
 * - **[AbilityFlag.REMOVE_COUNTER_TO_UNTAP]** (CR 614 replacement applied during
 *   the untap step, CR 502 — e.g. the creature enchanted by Bewitching
 *   Leechcraft): if [projected] is supplied (untap-step path only) and the
 *   permanent has this granted flag, the engine tries to remove a +1/+1 counter
 *   instead of untapping. The permanent untaps **only if** a counter was removed;
 *   with no +1/+1 counter present it stays tapped. This replacement is keyed to
 *   the natural untap step, so callers outside the untap step (explicit untap
 *   effects) pass `projected = null` and never apply it.
 *
 * Resolution order: stun counters are checked first (Rule 122.1d removes one stun
 * counter and stops), then the granted +1/+1-counter replacement.
 *
 * - If [entityId] is already untapped: no-op (the replacement only fires on a
 *   permanent that *would* become untapped — already-untapped permanents are not
 *   eligible).
 *
 * @param projected projected state used to read granted untap-step flags. Pass the
 *   untap-step projected state on the natural untap path; pass `null` for explicit
 *   "untap target" effects so the [AbilityFlag.REMOVE_COUNTER_TO_UNTAP] replacement
 *   does not apply.
 * @return the updated state plus an optional [UntappedEvent] (null when the
 *   permanent stayed tapped or wasn't tapped to begin with). When a +1/+1 counter
 *   is removed and the permanent untaps, a [CountersRemovedEvent] is also returned.
 */
fun untapOrConsumeStun(
    state: GameState,
    entityId: EntityId,
    projected: ProjectedState? = null,
): Pair<GameState, List<UntappedOrConsumeEvent>> {
    val container = state.getEntity(entityId) ?: return state to emptyList()
    if (!container.has<TappedComponent>()) return state to emptyList()

    // Rule 122.1d: a stun counter replaces becoming untapped by removing itself.
    val stunCounters = container.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0
    if (stunCounters > 0) {
        val newState = state.updateEntity(entityId) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withRemoved(CounterType.STUN, 1))
        }
        return newState to emptyList()
    }

    val cardName = container.get<CardComponent>()?.name ?: "Permanent"

    // Granted "remove a +1/+1 counter to untap" replacement (untap-step path only).
    if (projected != null && projected.hasKeyword(entityId, AbilityFlag.REMOVE_COUNTER_TO_UNTAP)) {
        val plusOneCounters = container.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
        if (plusOneCounters <= 0) {
            // No +1/+1 counter to remove — it doesn't untap.
            return state to emptyList()
        }
        val newState = state.updateEntity(entityId) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withRemoved(CounterType.PLUS_ONE_PLUS_ONE, 1)).without<TappedComponent>()
        }
        return newState to listOf(
            CountersRemovedEvent(entityId, CounterType.PLUS_ONE_PLUS_ONE.name, 1, cardName),
            UntappedEvent(entityId, cardName),
        )
    }

    val newState = state.updateEntity(entityId) { it.without<TappedComponent>() }
    return newState to listOf(UntappedEvent(entityId, cardName))
}

/** Marker for the events [untapOrConsumeStun] may emit ([UntappedEvent], [CountersRemovedEvent]). */
typealias UntappedOrConsumeEvent = GameEvent
