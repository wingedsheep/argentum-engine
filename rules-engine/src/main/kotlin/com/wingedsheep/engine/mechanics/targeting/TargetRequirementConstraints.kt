package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/** Attach shared owner-distribution and equality constraints to every targeting pause, including free casts. */
fun com.wingedsheep.engine.core.TargetRequirementInfo.withTargetConstraints(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val distributed = withOwnerDistribution(state, requirement, candidates).withDistinctCategories(state, requirement, candidates)
        .withDifferentValues(state, requirement, candidates)
    val property = DynamicTargetCount.objectOf(requirement)?.sameNumericProperty ?: return distributed
    val groups = NumericTargetEquality.compatibleGroups(state, candidates, requirement, distributed.minTargets).orEmpty()
    return distributed.copy(sameNumericProperty = property, selectionGroups = groups,
        maxTargets = minOf(distributed.maxTargets, groups.maxOfOrNull { group ->
            if (distributed.exclusiveGroups != null) OwnerTargetDistribution.groups(state, group).size else group.size
        } ?: 0))
}

/**
 * "… with different mana values": one exclusive pool per value (at most one pick from each), and the
 * count capped at the number of distinct values among the candidates.
 */
private fun com.wingedsheep.engine.core.TargetRequirementInfo.withDifferentValues(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val property = DynamicTargetCount.objectOf(requirement)?.differentNumericProperty ?: return this
    val groups = NumericTargetEquality.groups(state, candidates, property)
    return copy(exclusiveGroups = groups, maxTargets = minOf(maxTargets, groups.size))
}

/**
 * "Up to one target … of each card type / color": attach each candidate's categories and cap the
 * count at the largest selection whose picks can each claim a different one ([OnePerCategoryTargets]).
 */
private fun com.wingedsheep.engine.core.TargetRequirementInfo.withDistinctCategories(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val table = OnePerCategoryTargets.forCandidates(state, requirement, candidates) ?: return this
    return copy(distinctCategories = table, maxTargets = minOf(maxTargets, OnePerCategoryTargets.maxTargets(table)))
}

/**
 * A requirement's aggregate cap — [com.wingedsheep.sdk.scripting.targets.TargetObject.totalManaValueAtMost]
 * ("... with total mana value 6 or less", the default [cap]) or
 * [com.wingedsheep.sdk.scripting.targets.TargetObject.totalPowerAtMost] ("... with total power 10
 * or less") — baked to a concrete integer for a targeting pause, so the decision validator can hold
 * the selection to it. `null` when the requirement has no such cap or the amount can't be read in
 * [context].
 */
fun resolveTotalManaValueCap(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
    cap: (com.wingedsheep.sdk.scripting.targets.TargetObject) -> com.wingedsheep.sdk.scripting.values.DynamicAmount? =
        { it.totalManaValueAtMost },
    context: () -> com.wingedsheep.engine.handlers.EffectContext,
): Int? {
    val amount = (requirement as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.let(cap) ?: return null
    return try {
        evaluator.evaluate(state, amount, context()).coerceAtLeast(0)
    } catch (_: Exception) {
        null
    }
}

/**
 * Bake a requirement's [com.wingedsheep.sdk.scripting.targets.TargetObject.totalPowerAtMost] cap
 * into a targeting pause: the resolved cap plus each candidate's power ([powerTowardTotal]), so the
 * decision validator, the AI and the client all sum the same numbers the server does — a `*` power
 * card's characteristic-defining ability included. A no-op when the requirement has no such cap.
 */
fun com.wingedsheep.engine.core.TargetRequirementInfo.withTotalPowerCap(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
    evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
    context: () -> com.wingedsheep.engine.handlers.EffectContext,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val cap = resolveTotalManaValueCap(state, requirement, evaluator, { it.totalPowerAtMost }, context) ?: return this
    return copy(totalPowerAtMost = cap, targetPowers = candidates.associateWith { powerTowardTotal(state, it, evaluator) })
}

/**
 * The power a target counts toward a "total power N or less" cap: projected power on the
 * battlefield; in any other zone the card's own power, a `*` evaluated by its characteristic-defining
 * ability (CR 604.3); 0 for an object with no power.
 */
fun powerTowardTotal(
    state: GameState,
    entityId: EntityId,
    evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
): Int = state.projectedState.getPower(entityId)
    ?: evaluator.offBattlefieldStat(state, entityId, isPower = true)
    ?: 0
