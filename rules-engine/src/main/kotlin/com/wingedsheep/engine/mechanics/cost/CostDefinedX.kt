package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.ManaValues
import com.wingedsheep.engine.mechanics.targeting.NumericTargetEquality
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * Costs whose *pick* announces X — the value of a variable defined by a cost choice is announced
 * as the ability is activated (CR 601.2b via CR 602.2b), so an X-bounded target ("counter target
 * spell with mana value X") can only be chosen once the pick is known, and a pick that leaves no
 * legal target is an activation that can't be completed.
 *
 * Shared by the single-permanent [com.wingedsheep.sdk.scripting.costs.CostAtom.VariablePermanents]
 * cost ("sacrifice an artifact with mana value X" — Scrap Welder) and the measured
 * [AbilityCost.DiscardX] cost ("discard a card with mana value X" — Kozilek, the Great Distortion),
 * and by both the enumerator (the ability isn't offered when no pick is viable) and the
 * activation's selection pause (only viable picks are offered), so the two can't disagree.
 */
object CostDefinedX {

    /**
     * The [candidates] whose own X ([xOf]) leaves every mandatory controller-chosen requirement in
     * [targetRequirements] enough legal targets. With no mandatory controller target every
     * candidate is viable. Legality is checked once per distinct X.
     */
    fun picksLeavingTargets(
        state: GameState,
        candidates: List<EntityId>,
        xOf: (EntityId) -> Int,
        targetRequirements: List<TargetRequirement>,
        playerId: EntityId,
        sourceId: EntityId,
        targetFinder: TargetFinder,
    ): List<EntityId> {
        val mandatory = targetRequirements.filter { it.chooser == TargetChooser.Controller && it.effectiveMinCount > 0 }
        if (mandatory.isEmpty()) return candidates
        val viableByX = mutableMapOf<Int, Boolean>()
        return candidates.filter { id ->
            val x = xOf(id)
            viableByX.getOrPut(x) {
                val context = PredicateContext(controllerId = playerId, sourceId = sourceId, xValue = x)
                mandatory.all { req ->
                    val legal = targetFinder.findLegalTargets(
                        state, req, playerId, sourceId,
                        targetingSourceType = TargetingSourceType.ACTIVATED_ABILITY, pipelineContext = context
                    )
                    NumericTargetEquality.eligible(state, legal, req).size >= req.effectiveMinCount
                }
            }
        }
    }

    /**
     * The hand cards an [AbilityCost.DiscardX] cost may discard: [playerId]'s hand cards matching
     * the cost's filter, never the ability's own source (a card can't pay for itself while it is
     * the thing being activated). The one answer the enumerator, the selection pause and payment
     * share.
     */
    fun discardCandidates(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        cost: AbilityCost.DiscardX,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId> {
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        val projected = state.projectedState
        return state.getZone(ZoneKey(playerId, Zone.HAND)).filter {
            it != sourceId && predicateEvaluator.matches(state, projected, it, cost.filter, context)
        }
    }

    /**
     * The X a measured discard announces when an activation arrives with its discard already
     * chosen and no X ([discarded] non-empty, [announcedX] null) — the engine-direct path, where
     * the pick *is* the announcement. Null for any other cost, so the caller keeps its own X.
     */
    fun announcedByDiscard(
        state: GameState,
        cost: AbilityCost.DiscardX?,
        discarded: List<EntityId>?,
        announcedX: Int?,
    ): Int? =
        if (announcedX == null && cost?.count != null && !discarded.isNullOrEmpty()) discardX(state, cost, discarded)
        else null

    /** X as [cost] reads it off [discarded] hand cards — how many, or their total mana value. */
    fun discardX(state: GameState, cost: AbilityCost.DiscardX, discarded: List<EntityId>): Int =
        measureCards(state, cost.xMeasure, discarded)

    /** [measure] of [cards] read off the cards themselves (hand cards have no projection). */
    fun measureCards(state: GameState, measure: VariableCostMeasure, cards: List<EntityId>): Int =
        when (measure) {
            VariableCostMeasure.COUNT -> cards.size
            VariableCostMeasure.TOTAL_MANA_VALUE ->
                cards.sumOf { ManaValues.of(state, it) }
            VariableCostMeasure.TOTAL_POWER -> error("DiscardX never measures power")
        }

    /**
     * The hand cards a measured [cost] may really be paid with. A `{X}` mana symbol that already
     * fixed X ([fixedX]) admits only single cards measuring exactly X; otherwise the pick announces
     * X, so only cards leaving the ability a legal target are offered. A multi-card measured
     * discard is offered unfiltered (no printed card has one) and checked at payment.
     */
    fun viableDiscards(
        state: GameState,
        cost: AbilityCost.DiscardX,
        candidates: List<EntityId>,
        fixedX: Int?,
        targetRequirements: List<TargetRequirement>,
        playerId: EntityId,
        sourceId: EntityId,
        targetFinder: TargetFinder,
    ): List<EntityId> {
        if (cost.count != 1) return candidates
        val xOf = { id: EntityId -> discardX(state, cost, listOf(id)) }
        return if (fixedX != null) candidates.filter { xOf(it) == fixedX }
        else picksLeavingTargets(state, candidates, xOf, targetRequirements, playerId, sourceId, targetFinder)
    }
}
