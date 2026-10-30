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
 * Bake a requirement's [com.wingedsheep.sdk.scripting.targets.TargetObject.totalAtMost] cap into a
 * targeting pause: the cap resolved to an integer in [context], its property's label, and each
 * candidate's value ([TotalCaps.valueOf]) — so the decision validator, the AI and the client all sum
 * the numbers the server does, a `*` power card's characteristic-defining ability included. A no-op
 * when the requirement has no such cap or its amount can't be read in [context].
 */
fun com.wingedsheep.engine.core.TargetRequirementInfo.withTotalCap(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
    evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
    context: () -> com.wingedsheep.engine.handlers.EffectContext,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val totalCap = (requirement as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.totalAtMost ?: return this
    val cap = TotalCaps.resolve(state, totalCap, evaluator, context()) ?: return this
    return copy(
        totalCap = cap,
        totalCapLabel = totalCap.property.description,
        targetValues = candidates.associateWith { TotalCaps.valueOf(state, it, totalCap.property) },
    )
}

/** The one reading of a [com.wingedsheep.sdk.scripting.targets.TotalCap] — its cap and each target's share. */
object TotalCaps {
    /** The cap's amount resolved in [context], floored at 0; `null` when it can't be read there. */
    fun resolve(
        state: GameState,
        cap: com.wingedsheep.sdk.scripting.targets.TotalCap,
        evaluator: com.wingedsheep.engine.handlers.DynamicAmountEvaluator,
        context: com.wingedsheep.engine.handlers.EffectContext,
    ): Int? = try {
        evaluator.evaluate(state, cap.amount, context).coerceAtLeast(0)
    } catch (_: Exception) {
        null
    }

    /**
     * What one target adds to the sum: its current [property] value ([NumericTargetEquality.value] —
     * projected on the battlefield; off it a `*` stat is its characteristic-defining ability,
     * CR 604.3), 0 for an object without one (a player, a noncreature's power).
     */
    fun valueOf(state: GameState, id: EntityId, property: com.wingedsheep.sdk.scripting.values.CardNumericProperty): Int =
        NumericTargetEquality.value(state, id, property) ?: 0
}
