package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure

/**
 * The shared answer to "which permanents can pay a [CostAtom.VariablePermanents] cost, and how much
 * do the chosen ones measure" — every reader of *that atom* (the cast enumerators, the cast
 * validator, [com.wingedsheep.engine.handlers.CostHandler], and the built-in AI) goes through here,
 * so they can't disagree about affordability.
 *
 * The `TAP` + `TOTAL_POWER` shape is the one Teamwork N uses (CR 702.194a): "tap any number of
 * creatures you control with total power N or more" — the same selection crew (CR 702.122b) and
 * saddle already make, which is why [candidates] mirrors
 * `com.wingedsheep.engine.legalactions.enumerators.CrewEnumerator`: untapped, controlled by the
 * payer, matched through **projected** state, and with no summoning-sickness check (CR 302.6
 * governs the `{T}` symbol in an activation cost, not a tap paid as a cost).
 *
 * **Crew and saddle are not routed through here.** They are not `VariablePermanents` costs — they
 * are their own activation shape with their own action (`CrewVehicle`) — and they keep their own
 * copies of the eligibility filter in `CrewEnumerator` / `CrewVehicleHandler`. The eligibility rule
 * is therefore duplicated (it agrees today, and both cite the same rules), but the *measures*
 * genuinely differ and must stay separate: crew sums through `CrewSaddleContributionEvaluator`, so
 * a "crews Vehicles as though its power were 2 greater" static raises a crew total, and must not
 * raise a teamwork total (CR 702.194a measures printed-and-projected power, nothing crew-specific).
 * Migrating crew's candidate selection onto [candidates] is a worthwhile follow-up, but it is a
 * change to a second mechanic, not to teamwork.
 */
object VariablePermanentsCost {

    private val predicateEvaluator = PredicateEvaluator()

    /** Lower-case verb for this action, used in payment-failure messages and prompts. */
    fun verb(action: PermanentCostAction): String = when (action) {
        PermanentCostAction.EXILE -> "exile"
        PermanentCostAction.SACRIFICE -> "sacrifice"
        PermanentCostAction.TAP -> "tap"
    }

    /** Human name of the measured quantity, for "falls short of the required total power" errors. */
    fun measureName(measure: VariableCostMeasure): String = when (measure) {
        VariableCostMeasure.TOTAL_MANA_VALUE -> "total mana value"
        VariableCostMeasure.COUNT -> "count"
        VariableCostMeasure.TOTAL_POWER -> "total power"
    }

    /**
     * Measure [chosen] the way [measure] says — the value a `minMeasure` floor is compared against,
     * and the ability's X when it resolves (CR 601.2b).
     *
     * `TOTAL_MANA_VALUE` and `COUNT` read correctly even after the chosen permanents have left the
     * battlefield (mana value is intrinsic to the card; the count is a property of the selection).
     * `TOTAL_POWER` reads **projected** power, so a lord bonus or a +1/+1 counter counts toward a
     * teamwork threshold — and its permanents are only tapped, never moved, so they are still on
     * the battlefield when it is read.
     */
    fun measure(state: GameState, measure: VariableCostMeasure, chosen: List<EntityId>): Int =
        when (measure) {
            VariableCostMeasure.TOTAL_MANA_VALUE ->
                chosen.sumOf { state.getEntity(it)?.get<CardComponent>()?.manaValue ?: 0 }
            VariableCostMeasure.COUNT -> chosen.size
            VariableCostMeasure.TOTAL_POWER -> {
                val projected = state.projectedState
                chosen.sumOf { projected.getPower(it) ?: 0 }
            }
        }

    /**
     * Permanents [playerId] may choose to pay [atom], in battlefield order.
     *
     * A `TAP` atom sees only untapped permanents (CR 701.26a — "only untapped permanents can be
     * tapped"); the other actions see every match. [sourceId] is the cost's own source, excluded
     * when the atom sets `excludeSelf`; pass null for a spell's additional cost, which has no
     * source permanent on the battlefield.
     */
    fun candidates(
        state: GameState,
        playerId: EntityId,
        atom: CostAtom.VariablePermanents,
        sourceId: EntityId? = null,
    ): List<EntityId> {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        return projected.getBattlefieldControlledBy(playerId).filter { entityId ->
            if (atom.excludeSelf && entityId == sourceId) return@filter false
            val container = state.getEntity(entityId) ?: return@filter false
            container.get<CardComponent>() ?: return@filter false
            if (atom.action == PermanentCostAction.TAP && container.has<TappedComponent>()) return@filter false
            predicateEvaluator.matches(state, projected, entityId, atom.filter, context)
        }
    }

    /**
     * Tap every permanent in [chosen] to pay a [PermanentCostAction.TAP] variable-permanents cost,
     * stamping [reason] as the cause on each [com.wingedsheep.engine.core.TappedEvent].
     *
     * **The single tap site for this atom.** Both payers — the spell's additional cost
     * (`CastSpellHandler`) and the activated ability's cost (`CostHandler.payVariablePermanentsList`)
     * — go through here, so the two can't drift on what a tap paid for this atom emits. Each caller
     * validates its own selection first (control, filter, untapped, the measure floor) and then
     * hands the vetted ids over; this does the tapping and nothing else. Routed through the tap atom
     * [com.wingedsheep.engine.core.tap], so an already-tapped or vanished permanent contributes no
     * event (CR 701.26a) and "whenever this becomes tapped" triggers fire exactly once each.
     *
     * The permanents are only tapped, never moved, so no last-known-information snapshot is needed.
     */
    fun tapAll(
        state: GameState,
        chosen: List<EntityId>,
        reason: TapReason,
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for (id in chosen) {
            val (tappedState, tapEvent) = tap(newState, id, reason = reason)
            newState = tappedState
            tapEvent?.let(events::add)
        }
        return newState to events
    }

    /**
     * True when [playerId] can pay [atom] at all — enough candidates to clear the count floor, and
     * a reachable ceiling that clears the measure floor. Used to mark a cast variant unaffordable
     * rather than offering a cast the caster can't complete (CR 601.2h).
     *
     * The ceiling is the sum of the candidates' *non-negative* contributions, not the measure of
     * the whole candidate list: the payer picks any subset (CR 702.194a — "any number of creatures
     * you control with total power N or more"), so a creature at negative power (Weakness, a
     * −1/−1 counter, an opposing lord) is simply left out rather than dragging the total down.
     * Summing the raw list instead would report a 3/3 plus a −2/2 as unable to pay teamwork 2.
     * Only `TOTAL_POWER` can go negative; the other measures are unaffected by the clamp.
     */
    fun canPay(
        state: GameState,
        playerId: EntityId,
        atom: CostAtom.VariablePermanents,
        sourceId: EntityId? = null,
    ): Boolean {
        val candidates = candidates(state, playerId, atom, sourceId)
        if (candidates.size < atom.minCount) return false
        if (atom.minMeasure <= 0) return true
        val reachable = candidates.sumOf { id ->
            maxOf(0, measure(state, atom.xMeasure, listOf(id)))
        }
        return reachable >= atom.minMeasure
    }
}
