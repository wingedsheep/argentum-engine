package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId

/** Choose inside one compatible pool, with at most one candidate per exclusive group. */
fun constrainedTargetSelection(
    pool: List<EntityId>,
    minimum: Int,
    maximum: Int,
    compatibleGroups: List<List<EntityId>>?,
    exclusiveGroups: List<List<EntityId>>,
    rank: (EntityId) -> Double = { 0.0 },
): List<EntityId> {
    val candidates = compatibleGroups ?: listOf(pool)
    return candidates.mapNotNull { group ->
        val allowed = group.filterTo(mutableSetOf()) { it in pool }
        val picks = exclusiveGroups.mapNotNull { owner -> owner.filter { it in allowed }.maxByOrNull(rank) }
            .sortedByDescending(rank)
            .let { if (minimum == 0) it.filter { id -> rank(id) > 0.0 } else it }
            .take(maximum)
        picks.takeIf { it.size >= minimum }
    }.maxByOrNull { picks -> picks.sumOf(rank) }.orEmpty()
}
