package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * The group rule behind [CostAtom.TapPermanents.sharedCreatureType] — "tap two untapped creatures
 * you control that share a creature type" (Weight of Conscience). One rule, read by every path a
 * tap cost travels (affordability, offered candidates, payment validation), so they can't disagree.
 *
 * Creature types are read off *projected* state, so a changeling (all creature types) or a
 * type-granting effect counts.
 */
internal object SharedCreatureTypeTapCost {

    /**
     * Narrow [candidates] to the permanents that can be part of a legal payment: those holding a
     * creature type at least [CostAtom.TapPermanents.count] candidates share. Every caller's
     * `pool.size >= count` affordability check stays correct on the narrowed pool — any survivor's
     * type is held by `count` survivors. The largest group leads the list, so a payer that takes
     * the first `count` candidates (the AI) picks a legal set. A no-op unless the axis is set.
     */
    fun eligible(state: GameState, atom: CostAtom.TapPermanents, candidates: List<EntityId>): List<EntityId> {
        if (!atom.sharedCreatureType) return candidates
        val typesById = candidates.associateWith { creatureTypes(state, it) }
        val groupSizes = typesById.values.flatten().groupingBy { it }.eachCount()
        val pool = candidates.filter { id -> typesById.getValue(id).any { (groupSizes[it] ?: 0) >= atom.count } }
        val largest = groupSizes.maxByOrNull { it.value }?.key ?: return pool
        return pool.sortedByDescending { largest in typesById.getValue(it) }
    }

    /** Whether the chosen permanents satisfy the axis: all of them have a creature type in common. */
    fun satisfiedBy(state: GameState, atom: CostAtom.TapPermanents, chosen: Collection<EntityId>): Boolean =
        satisfiedBy(state, atom.sharedCreatureType, chosen)

    /** [satisfiedBy] for a path that carried only the axis across a pause (pay-or-suffer). */
    fun satisfiedBy(state: GameState, sharedCreatureType: Boolean, chosen: Collection<EntityId>): Boolean {
        if (!sharedCreatureType || chosen.size < 2) return true
        return chosen.map { creatureTypes(state, it) }.reduce { acc, next -> acc intersect next }.isNotEmpty()
    }

    private fun creatureTypes(state: GameState, entityId: EntityId): Set<String> =
        state.projectedState.getSubtypes(entityId).filterTo(mutableSetOf()) { it in CREATURE_TYPES }

    private val CREATURE_TYPES: Set<String> = Subtype.ALL_CREATURE_TYPES.toSet()
}
