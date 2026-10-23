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
 * A requirement's [com.wingedsheep.sdk.scripting.targets.TargetObject.totalManaValueAtMost] cap
 * ("... with total mana value 6 or less", CR 601.2c) baked to a concrete integer for a targeting
 * pause, so the decision validator can hold the selection to it. `null` when the requirement has
 * no such cap or the amount can't be read in [context].
 */
fun resolveTotalManaValueCap(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
    context: () -> com.wingedsheep.engine.handlers.EffectContext,
): Int? {
    val cap = (requirement as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.totalManaValueAtMost ?: return null
    return try {
        evaluator.evaluate(state, cap, context()).coerceAtLeast(0)
    } catch (_: Exception) {
        null
    }
}
