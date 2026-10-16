package com.wingedsheep.engine.core

import com.wingedsheep.engine.mechanics.targeting.OnePerCardType
import com.wingedsheep.sdk.model.EntityId

/**
 * Choose inside one compatible pool, with at most one candidate per exclusive group and — when
 * [distinctCategories] is set ("up to one target … of each color") — only picks that can each claim
 * a different category. A candidate missing from [distinctCategories] can claim none and is skipped.
 */
fun constrainedTargetSelection(
    pool: List<EntityId>,
    minimum: Int,
    maximum: Int,
    compatibleGroups: List<List<EntityId>>?,
    exclusiveGroups: List<List<EntityId>>?,
    rank: (EntityId) -> Double = { 0.0 },
    distinctCategories: Map<EntityId, List<String>>? = null,
): List<EntityId> {
    val candidates = compatibleGroups ?: listOf(pool)
    return candidates.mapNotNull { group ->
        val allowed = group.filterTo(mutableSetOf()) { it in pool }
        val ranked = (exclusiveGroups?.mapNotNull { owner -> owner.filter { it in allowed }.maxByOrNull(rank) } ?: allowed.toList())
            .sortedByDescending(rank)
            .let { if (minimum == 0) it.filter { id -> rank(id) > 0.0 } else it }
        val picks = if (distinctCategories == null) ranked.take(maximum) else {
            val claimed = mutableListOf<Set<String>>()
            ranked.filter { id ->
                val categories = distinctCategories[id]?.toSet()
                claimed.size < maximum && categories != null &&
                    OnePerCardType.canAssignDistinct(claimed + listOf(categories)).also { if (it) claimed += categories }
            }
        }
        picks.takeIf { it.size >= minimum }
    }.maxByOrNull { picks -> picks.sumOf(rank) }.orEmpty()
}
