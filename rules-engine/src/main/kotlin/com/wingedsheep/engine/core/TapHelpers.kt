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
 * Tap a single permanent on the battlefield, emitting the [TappedEvent] that
 * "whenever this becomes tapped" triggers (Station, Cryptic Gateway, …) and the
 * client tap animation react to.
 *
 * This is the canonical *tap atom* — the analogue of [untapOrConsumeStun] for the
 * untap direction and of `DamageUtils.gainLife` for life. Route every place a
 * permanent *becomes tapped while on the battlefield* through here so the mutation
 * and its event can never drift apart. Past bugs (station creatures, declare-attackers)
 * came from open-coding `with(TappedComponent)` and forgetting the paired event;
 * `TapEventEnforcementTest` now bans that pattern outside the legitimate
 * enters-tapped/cleanup sites.
 *
 * Tapping is a transition (CR 603.2f): a permanent that is *already* tapped does not
 * become tapped again, so this is a no-op that emits no event. The same is true for an
 * entity that no longer exists. In both cases the original [state] is returned paired
 * with `null`, so callers can fold the event in without a special case:
 *
 * ```
 * val (next, event) = tap(state, id)
 * return EffectResult.success(next, listOfNotNull(event))
 * ```
 *
 * **Not for permanents entering tapped.** A permanent that *enters the battlefield
 * tapped* (taplands, tokens created tapped, phased-in-tapped, sneak/regeneration) is
 * not transitioning from untapped to tapped, so those sites set [TappedComponent]
 * directly and emit no event — they are the allowlist in `TapEventEnforcementTest`.
 *
 * @return the updated state paired with the emitted [TappedEvent], or `state to null`
 *   when the permanent was already tapped or doesn't exist (no mutation performed).
 */
fun tap(state: GameState, entityId: EntityId): Pair<GameState, TappedEvent?> {
    val container = state.getEntity(entityId) ?: return state to null
    // CR 603.2f: tapping an already-tapped permanent is not a transition — no event.
    if (container.has<TappedComponent>()) return state to null
    val cardName = container.get<CardComponent>()?.name ?: "Permanent"
    val newState = state.updateEntity(entityId) { it.with(TappedComponent) }
    return newState to TappedEvent(entityId, cardName)
}

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

    // "Can't become untapped" (e.g. Blossombind) — the stronger continuous restriction
    // that blocks *every* untap source, not only the untap step (contrast DOESNT_UNTAP,
    // CR 502.3). Because this atom is the single chokepoint for untapping, checking it
    // here covers explicit untap effects, provoke, untap costs, and the untap step alike.
    // Read off the always-cached projected state so it applies even when a caller passed
    // projected = null (explicit "untap target" path). A permanent that can't untap never
    // "would become untapped", so this precedes the stun/counter replacements (which only
    // fire on a permanent that would otherwise untap).
    if (state.projectedState.hasKeyword(entityId, AbilityFlag.CANT_BECOME_UNTAPPED)) {
        return state to emptyList()
    }

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

/**
 * True if [entityId] must be skipped by an untap step (CR 502.3) — either the narrow
 * [AbilityFlag.DOESNT_UNTAP] ("doesn't untap during your untap step") or the stronger
 * [AbilityFlag.CANT_BECOME_UNTAPPED], which also blocks untap *effects* and therefore
 * subsumes the untap-step behavior. Use this at every untap-step gate in
 * [BeginningPhaseManager] so both restrictions drop the permanent from the untap list
 * (and from the MAY_NOT_UNTAP / untap-limit choice pools). Universal untap enforcement
 * still lives in [untapOrConsumeStun]; this is the untap-step-only convenience.
 */
fun ProjectedState.doesntUntapDuringUntapStep(entityId: EntityId): Boolean =
    hasKeyword(entityId, AbilityFlag.DOESNT_UNTAP) ||
        hasKeyword(entityId, AbilityFlag.CANT_BECOME_UNTAPPED)
