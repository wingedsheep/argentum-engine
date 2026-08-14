package com.wingedsheep.engine.core

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TapReason

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
 * Tapping is a transition (CR 701.26a — "only untapped permanents can be tapped"):
 * a permanent that is *already* tapped does not
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
 * **Attribution.** [TappedEvent.tappedById] records *who tapped it*, which the "whenever you tap a
 * creature an opponent controls" trigger family reads. It defaults to the permanent's own
 * controller, which is already correct for every tap a permanent's controller performs on it — a
 * cost payment (convoke, crew, saddle, a tap symbol in an activation cost), a mana ability, or the
 * turn-based action of declaring it as an attacker. Only an *effect* that taps a permanent needs to
 * pass [tappedById] explicitly: there the tapper is the controller of that effect, not of the
 * permanent, and the two differ exactly when it matters. Pass the player the game instructs to tap,
 * not the controller of the card that gave the instruction — a spell you control that makes an
 * opponent tap their own creature is *their* tap (Tangle Wire; Hylda of the Icy Crown ruling).
 *
 * The default reads the controller from **projected** state, so a permanent whose control was changed
 * (Threaten, Mind Control) attributes its cost/attack taps to the player actually wielding it rather
 * than to its owner. That costs a projection on a state instance that may not have one cached yet —
 * deliberate, and the same price [untapOrConsumeStun] already pays for its can't-untap check. It is
 * bounded by how many permanents one action taps (a mana payment, a combat declaration), not by board
 * size; if it ever shows up in a profile, the fix is to pass [tappedById] at the tapping call sites
 * — every one of them knows the acting player — not to fall back to base [ControllerComponent],
 * which would misattribute a stolen creature's taps to its owner.
 *
 * **Cause.** [TappedEvent.reason] records *why* it was tapped, which "becomes tapped to pay a
 * teamwork cost" (Agent Maria Hill) reads. It is a separate axis from [tappedById] — a teamwork tap
 * and an attack tap are both performed by the permanent's own controller, so attribution alone can
 * never tell them apart. It defaults to [TapReason.UNSPECIFIED] and stays there for every tap site
 * the engine has not been taught to name: an unclassified cause must never masquerade as a
 * classified one, because a card reading that cause would then fire wrongly. Only the teamwork
 * additional-cost payment classifies itself today; see [TapReason] for how to add the next cause.
 *
 * @param tappedById the player causing the tap; null (the default) attributes it to the permanent's
 *   controller.
 * @param reason why the permanent is becoming tapped; [TapReason.UNSPECIFIED] (the default) leaves
 *   the cause unnamed.
 * @return the updated state paired with the emitted [TappedEvent], or `state to null`
 *   when the permanent was already tapped or doesn't exist (no mutation performed).
 */
fun tap(
    state: GameState,
    entityId: EntityId,
    tappedById: EntityId? = null,
    reason: TapReason = TapReason.UNSPECIFIED,
): Pair<GameState, TappedEvent?> {
    val container = state.getEntity(entityId) ?: return state to null
    // CR 701.26a: only untapped permanents can be tapped, so tapping an already-tapped
    // permanent is not a transition — no event.
    if (container.has<TappedComponent>()) return state to null
    val cardName = container.get<CardComponent>()?.name ?: "Permanent"
    val tapper = tappedById
        ?: state.projectedState.getController(entityId)
        ?: container.get<ControllerComponent>()?.playerId
    val newState = state.updateEntity(entityId) { it.with(TappedComponent) }
    return newState to TappedEvent(entityId, cardName, tapper, reason)
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
