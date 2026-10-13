package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/** Attach shared owner-distribution and equality constraints to every targeting pause, including free casts. */
fun com.wingedsheep.engine.core.TargetRequirementInfo.withTargetConstraints(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val distributed = withOwnerDistribution(state, requirement, candidates)
    val property = DynamicTargetCount.objectOf(requirement)?.sameNumericProperty ?: return distributed
    val groups = NumericTargetEquality.compatibleGroups(state, candidates, requirement, distributed.minTargets).orEmpty()
    return distributed.copy(sameNumericProperty = property, selectionGroups = groups,
        maxTargets = minOf(distributed.maxTargets, groups.maxOfOrNull { group ->
            if (distributed.exclusiveGroups != null) OwnerTargetDistribution.groups(state, group).size else group.size
        } ?: 0))
}
