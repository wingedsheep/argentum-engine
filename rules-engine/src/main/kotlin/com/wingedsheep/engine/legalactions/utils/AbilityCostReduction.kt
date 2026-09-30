package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * [ActivatedAbility.genericCostReduction] applied to the mana portion of an ability cost, for the
 * *enumeration* side — the affordability gate, the cost string the client shows, and the auto-tap
 * preview. [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler] runs its own
 * copy at payment time against the target the player actually chose.
 *
 * Shared because the reduction is a property of the ability, not of the zone it is activated from:
 * [com.wingedsheep.engine.legalactions.enumerators.ActivatedAbilityEnumerator] applies it to
 * battlefield abilities and
 * [com.wingedsheep.engine.legalactions.enumerators.ZoneActivatedAbilityEnumerator] to the hand and
 * graveyard ones (the Kamigawa channel lands — "costs {1} less to activate for each legendary
 * creature you control" — are activated by discarding them *from hand*). Before this was shared,
 * the zone enumerator gated affordability on the unreduced cost and so hid a channel ability the
 * handler would have been happy to charge the reduced price for.
 */
object AbilityCostReduction {

    /**
     * [cost] with [ActivatedAbility.genericCostReduction] applied to its generic mana, evaluated
     * against the activating entity (e.g. the equipped creature for The Dominion Bracelet, where
     * X = the creature's power).
     *
     * When the ability requires a target, the player hasn't chosen one yet at enumeration time, so
     * a reduction that reads the chosen target (e.g. Dragonfire Blade — "costs {1} less to activate
     * for each color of the creature it targets") can't resolve a specific target here. We gate
     * affordability on the *cheapest* reachable cost — the largest reduction over the currently
     * legal targets — so the ability is offered whenever it's payable for at least one target. The
     * handler re-derives the exact reduction from the chosen target, and in auto-tap mode pays that
     * exact per-target cost. The reduction only ever lowers the cost, so a best-case preview never
     * causes the client to under-tap for the chosen target.
     */
    fun apply(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        targetUtils: TargetEnumerationUtils,
        predicateEvaluator: PredicateEvaluator
    ): AbilityCost {
        val afterConditional = applyConditional(cost, ability, state, sourceId, controllerId, predicateEvaluator.conditions)
        val reduction = ability.genericCostReduction ?: return afterConditional
        val evaluator = predicateEvaluator.amounts
        val amount = if (ability.targetRequirements.isNotEmpty()) {
            maxReductionOverLegalTargets(reduction, ability, state, sourceId, controllerId, targetUtils, evaluator)
        } else {
            evaluator.evaluate(state, reduction, EffectContext(sourceId = sourceId, controllerId = controllerId))
        }
        if (amount <= 0) return afterConditional
        return reduceGeneric(afterConditional, amount)
    }

    /**
     * [cost] with [ActivatedAbility.conditionalCostReduction] subtracted pip-wise (CR 118.7) from
     * its first mana component while the reduction's condition holds for [sourceId] and
     * [controllerId]. Shared by the enumerators (through [apply]) and the activation handler's
     * totaller, so the offered price and the charged price never disagree. A cost with no mana
     * component is returned unchanged — a reduction can only shrink mana already there.
     */
    fun applyConditional(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        conditions: ConditionEvaluator
    ): AbilityCost {
        val conditional = ability.conditionalCostReduction ?: return cost
        val context = EffectContext(sourceId = sourceId, controllerId = controllerId)
        if (!conditions.evaluate(state, conditional.condition, context)) return cost
        return mapFirstMana(cost) { it.subtract(conditional.reduction) }
    }

    /**
     * Largest [reduction] achievable across the ability's currently-legal first-requirement
     * targets. Evaluates the reduction once per legal target (as if that target were chosen) and
     * keeps the maximum. For a reduction that doesn't read the target this collapses to a constant,
     * so it stays correct for non-target-dependent reductions on targeted abilities too — which is
     * the channel lands' case, where the count is of legendary creatures you control. Returns 0
     * when there are no legal targets (the ability won't be offered anyway).
     */
    private fun maxReductionOverLegalTargets(
        reduction: DynamicAmount,
        ability: ActivatedAbility,
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        targetUtils: TargetEnumerationUtils,
        evaluator: DynamicAmountEvaluator
    ): Int {
        val validTargets = targetUtils
            .buildTargetInfos(
                state, controllerId, ability.targetRequirements, sourceId = sourceId,
                targetingSourceType = com.wingedsheep.engine.handlers.TargetingSourceType.ACTIVATED_ABILITY
            )
            .firstOrNull()?.validTargets ?: emptyList()
        if (validTargets.isEmpty()) return 0
        return validTargets.maxOf { targetId ->
            evaluator.evaluate(
                state,
                reduction,
                EffectContext(
                    sourceId = sourceId,
                    controllerId = controllerId,
                    targets = listOf(ChosenTarget.Permanent(targetId))
                )
            )
        }
    }

    /**
     * The menu label for [ability] given the [effectiveCost] it will actually be charged.
     *
     * Rebuilt from the effective cost only when that differs from [baselineCost] — otherwise the
     * printed label is already right — and never when the ability carries an explicit
     * `descriptionOverride`, since a custom label such as Renew's "Renew — …" has no safe place to
     * splice a cost into. Both enumerators render the label the same way, so the rule lives here
     * rather than in each of them.
     *
     * [baselineCost] defaults to the printed cost. The battlefield enumerator passes its
     * text-replaced cost instead ("Sacrifice a Goblin" → "Sacrifice a Bird"), so that a replacement
     * alone — which does not change what the player pays — doesn't count as a cost change.
     */
    fun describe(
        ability: ActivatedAbility,
        effectiveCost: AbilityCost,
        baselineCost: AbilityCost = ability.cost,
    ): String =
        if (effectiveCost != baselineCost && ability.descriptionOverride == null) {
            ability.describeWithCost(effectiveCost)
        } else {
            ability.description
        }

    /** [cost] with [amount] shaved off the generic part of its first mana component. */
    private fun reduceGeneric(cost: AbilityCost, amount: Int): AbilityCost =
        mapFirstMana(cost) { it.reduceGeneric(amount) }

    /** [cost] with its first mana component rewritten through [modify]; unchanged if it has none. */
    private fun mapFirstMana(cost: AbilityCost, modify: (ManaCost) -> ManaCost): AbilityCost = when (cost) {
        is AbilityCost.Atom -> cost.manaCostOrNull
            ?.let { AbilityCost.Atom(CostAtom.Mana(modify(it))) } ?: cost
        is AbilityCost.Composite -> {
            var applied = false
            AbilityCost.Composite(cost.costs.map { sub ->
                val subMana = sub.manaCostOrNull
                if (!applied && subMana != null) {
                    applied = true
                    AbilityCost.Atom(CostAtom.Mana(modify(subMana)))
                } else sub
            })
        }
        else -> cost
    }
}
