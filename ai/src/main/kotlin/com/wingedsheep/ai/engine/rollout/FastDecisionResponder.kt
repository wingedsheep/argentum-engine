package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.TargetSelection
import com.wingedsheep.ai.engine.TrivialDecisions
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.AssignDamageDecision
import com.wingedsheep.engine.core.BatchYesNoDecision
import com.wingedsheep.engine.core.BatchYesNoResponse
import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseModeDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageAssignmentResponse
import com.wingedsheep.engine.core.DamageEdgeAmount
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.ModesChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PilesSplitResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.SplitPilesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Answers a `PendingDecision` inside a playout, in O(1), with no simulation.
 *
 * `DecisionResponder` — the one the real AI uses — answers several decision types by *simulating*
 * the alternatives and scoring them. That is exactly right at a real decision and exactly wrong
 * inside a playout: a playout that simulates makes the rollout quadratic, and the whole reason a
 * rollout beats one static evaluation is that it is cheap enough to run many times. Every rule here
 * is a constant-time read of the decision object.
 *
 * The `when` is **exhaustive over the sealed `PendingDecision`**, deliberately: a new decision type
 * should be a compile error here, not a playout that silently dies mid-resolution and scores the
 * position it happened to stop at. Same guarantee the engine's executor registries give.
 *
 * Forced decisions route to [TrivialDecisions] first, so a playout answers them exactly as
 * `GameSimulator` does and rollout scores stay comparable with the static ones they replace.
 */
class FastDecisionResponder(private val intents: IntentCatalog = IntentCatalog.NONE) {

    /**
     * @param playerId the *rollout owner*, not necessarily the decision's player: target ranking is
     *   only meaningful from someone's perspective, and using the decider's own perspective would
     *   need a per-seat evaluator a playout does not have. The decision's own player is who the
     *   engine bills the choice to either way.
     */
    fun respond(state: GameState, decision: PendingDecision, playerId: EntityId): DecisionResponse {
        TrivialDecisions.responseFor(decision)?.let { return it }
        return when (decision) {
            // Rank by the same heuristic the Strategist's cheap path uses. Not "first legal": a
            // playout that aims every removal spell at the nearest 1/1 systematically undervalues
            // removal, which is a bias in the leaf the whole phase exists to improve.
            is ChooseTargetsDecision -> TargetsResponse(
                decision.id,
                decision.targetRequirements.associate { req ->
                    val legal = decision.legalTargets[req.index] ?: emptyList()
                    val count = req.maxTargets.coerceAtMost(legal.size).coerceAtLeast(
                        req.minTargets.coerceAtMost(legal.size)
                    )
                    req.index to legal
                        .sortedByDescending { TargetSelection.rank(state, it, playerId, intents) }
                        .take(count)
                }
            )

            // Take the minimum a selection demands. Selections are overwhelmingly costs (discard,
            // sacrifice) rather than benefits, and a playout that over-pays every cost biases every
            // line that contains one. The exception is a `minTotalManaValue` floor (collect
            // evidence N, CR 701.59a): that gate is a *sum*, so "the minimum count" is an illegal
            // submission the validator rejects — pay it with the fewest cards instead.
            is SelectCardsDecision ->
                if (decision.minTotalManaValue != null) {
                    CardsSelectedResponse(decision.id, manaFloorSelection(state, decision))
                } else {
                    CardsSelectedResponse(
                        decision.id,
                        decision.options.take(decision.minSelections.coerceAtMost(decision.options.size))
                    )
                }

            // A search is a benefit, so take as much as it offers.
            is SearchLibraryDecision -> CardsSelectedResponse(
                decision.id,
                decision.options.take(decision.maxSelections.coerceAtMost(decision.options.size))
            )

            // "You may" is on the card because doing it is usually good.
            is YesNoDecision -> YesNoResponse(decision.id, choice = true)
            is BatchYesNoDecision -> BatchYesNoResponse(decision.id, choice = true, applyToAll = true)

            is ChooseModeDecision -> {
                val available = decision.modes.filter { it.available }
                ModesChosenResponse(
                    decision.id,
                    available.take(decision.minModes.coerceAtLeast(1).coerceAtMost(available.size))
                        .map { it.index }
                )
            }

            is ChooseColorDecision ->
                ColorChosenResponse(decision.id, decision.availableColors.first())

            // The minimum. A number decision is as often "how much do you pay" as "how much do you
            // get", and the low end is the one that cannot make a line illegal.
            is ChooseNumberDecision -> NumberChosenResponse(decision.id, decision.minValue)

            // Everything on the first target that will take it, minimums honoured first.
            is DistributeDecision -> DistributionResponse(decision.id, distribute(decision))

            // Order is a real tactical choice (damage assignment order, scry). A playout takes the
            // engine's order rather than pretending to solve it.
            is OrderObjectsDecision -> OrderedResponse(decision.id, decision.objects)
            is ReorderLibraryDecision -> OrderedResponse(decision.id, decision.cards)

            is SplitPilesDecision -> PilesSplitResponse(decision.id, splitEvenly(decision))

            is ChooseOptionDecision -> OptionChosenResponse(decision.id, 0)

            is ChooseReplacementDecision -> ReplacementChosenResponse(
                decision.id,
                fromIndex = decision.defaultFromIndex ?: 0,
                toIndex = decision.allowedToByFrom.getOrNull(decision.defaultFromIndex ?: 0)
                    ?.firstOrNull() ?: 0,
            )

            // Spend the budget greedily, cheapest mode first. The `> 0` guard is load-bearing: a
            // zero-cost mode spun `DecisionResponder.respondBudgetModal` forever until Phase 0
            // fixed it, and a playout is the one place that hang would be invisible.
            is BudgetModalDecision -> BudgetModalResponse(decision.id, budgetModes(decision))

            // The engine already computed lethal-first assignments; confirm them.
            is AssignDamageDecision -> DamageAssignmentResponse(
                decision.id,
                decision.defaultAssignments.ifEmpty { decision.minimumAssignments }
            )
            is CombatResolutionDecision -> CombatResolutionResponse(
                decision.id,
                decision.edges.map { DamageEdgeAmount(it.id, it.amount) }
            )

            // Auto-pay when the solver found a payment; otherwise submit nothing, which the engine
            // reads as a decline and ends the line cleanly rather than looping on it.
            is SelectManaSourcesDecision ->
                if (decision.autoPaySuggestion.isNotEmpty()) {
                    ManaSourcesSelectedResponse(decision.id, autoPay = true)
                } else {
                    ManaSourcesSelectedResponse(decision.id, declined = true)
                }
        }
    }

    /** All of [DistributeDecision.totalAmount] on the first target that will take it. */
    private fun distribute(decision: DistributeDecision): Map<EntityId, Int> {
        if (decision.targets.isEmpty()) return emptyMap()
        val assignment = mutableMapOf<EntityId, Int>()
        var remaining = decision.totalAmount
        // Minimums first — a distribution short of one is rejected outright.
        for (target in decision.targets) {
            val min = decision.minPerTarget.coerceAtMost(remaining)
            if (min > 0) {
                assignment[target] = min
                remaining -= min
            }
        }
        for (target in decision.targets) {
            if (remaining <= 0) break
            val cap = decision.maxPerTarget[target] ?: decision.totalAmount
            val already = assignment[target] ?: 0
            val extra = (cap - already).coerceAtMost(remaining)
            if (extra > 0) {
                assignment[target] = already + extra
                remaining -= extra
            }
        }
        return assignment
    }

    /** Deal the cards round-robin into [SplitPilesDecision.numberOfPiles] piles. */
    private fun splitEvenly(decision: SplitPilesDecision): List<List<EntityId>> {
        val piles = List(decision.numberOfPiles.coerceAtLeast(1)) { mutableListOf<EntityId>() }
        decision.cards.forEachIndexed { index, card -> piles[index % piles.size].add(card) }
        return piles
    }

    /**
     * Satisfy a `minTotalManaValue` floor with the fewest cards — highest mana values first, the
     * same choice `CollectEvidenceResolver.autoSelect` makes. Returns nothing when the pool can't
     * reach the floor, which CR 701.59b makes the only legal answer anyway (and which the engine's
     * validator exempts from the floor, so an optional collection reads as a decline).
     */
    private fun manaFloorSelection(state: GameState, decision: SelectCardsDecision): List<EntityId> {
        val floor = decision.minTotalManaValue ?: return emptyList()
        val selected = mutableListOf<EntityId>()
        var total = 0
        val byValueDesc = decision.options.sortedByDescending { manaValueOf(state, it) }
        for (cardId in byValueDesc) {
            if (total >= floor || selected.size >= decision.maxSelections) break
            selected.add(cardId)
            total += manaValueOf(state, cardId)
        }
        return if (total >= floor) selected else emptyList()
    }

    private fun manaValueOf(state: GameState, cardId: EntityId): Int =
        state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0

    /** Greedy cheapest-first spend of a pawprint budget, free modes taken exactly once. */
    private fun budgetModes(decision: BudgetModalDecision): List<Int> {
        val chosen = mutableListOf<Int>()
        var remaining = decision.budget
        val byCost = decision.modes.withIndex().sortedBy { it.value.cost }
        for ((index, mode) in byCost) {
            if (mode.cost > remaining) continue
            chosen += index
            remaining -= mode.cost
            if (remaining <= 0) break
        }
        return chosen
    }
}
