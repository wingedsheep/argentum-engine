package com.wingedsheep.engine.mechanics.modal

import com.wingedsheep.sdk.model.EntityId

/**
 * Feasibility for [com.wingedsheep.sdk.scripting.effects.ModalEffect.eachModeTargetsDifferent] —
 * "Each mode must target a different player."
 *
 * Every targeting mode claims a target no other chosen mode may share, so whether a set of modes
 * can be chosen together is a bipartite matching of modes to targets. A mode's *pool* is the union
 * of its requirements' legal targets; a mode with no mandatory target requirement has a `null`
 * pool and claims nothing. Each claiming mode is counted as taking one target — exact for every
 * printed card with this wording, whose modes each have a single "target player/opponent"; a mode
 * with several mandatory targets would need one slot per target here. Mode menus and target
 * pools are tiny (a handful of modes, the players at the table), so a plain augmenting-path
 * matching is all this needs.
 */
internal object DistinctModeTargets {

    /** Can each pool in [pools] get its own target, none of them in [taken]? */
    fun assignable(pools: List<Set<EntityId>?>, taken: Set<EntityId> = emptySet()): Boolean {
        val claiming = pools.filterNotNull().map { it - taken }
        if (claiming.any { it.isEmpty() }) return false
        val owner = mutableMapOf<EntityId, Int>()
        fun augment(mode: Int, seen: MutableSet<EntityId>): Boolean {
            for (target in claiming[mode]) {
                if (!seen.add(target)) continue
                val holder = owner[target]
                if (holder == null || augment(holder, seen)) {
                    owner[target] = mode
                    return true
                }
            }
            return false
        }
        return claiming.indices.all { augment(it, mutableSetOf()) }
    }

    /**
     * Can [selected] be grown, from [candidates], to at least [needed] modes that are still
     * [assignable]? [selected] alone must be assignable even when it already meets [needed].
     */
    fun completable(
        selected: List<Int>,
        candidates: List<Int>,
        needed: Int,
        poolOf: (Int) -> Set<EntityId>?
    ): Boolean {
        val base = selected.map(poolOf)
        val missing = needed - selected.size
        if (missing <= 0) return assignable(base)
        val rest = candidates.filter { it !in selected }
        if (rest.size < missing) return false
        fun pick(from: Int, chosen: List<Set<EntityId>?>): Boolean {
            if (chosen.size == missing) return assignable(base + chosen)
            for (i in from until rest.size) {
                if (pick(i + 1, chosen + listOf(poolOf(rest[i])))) return true
            }
            return false
        }
        return pick(0, emptyList())
    }
}
