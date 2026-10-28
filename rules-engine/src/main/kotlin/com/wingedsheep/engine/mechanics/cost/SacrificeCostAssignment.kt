package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * Several fixed-count [CostAtom.Sacrifice] atoms in one composite cost — Jarad, Golgari Lich Lord's
 * "Sacrifice a Swamp and a Forest" — are paid by **one disjoint assignment**: every atom gets its own
 * permanents, and no permanent pays two atoms (a permanent sacrificed once is gone). A Swamp Forest
 * dual can pay either atom, never both.
 *
 * Shape on the wire: the chosen permanents ride in the one flat `sacrificedPermanents` list as
 * **ordered segments** — the first atom's `count` permanents, then the next atom's, in the order the
 * atoms appear in the cost ([segments]). A cost with a single sacrifice atom is one segment, so
 * nothing changes for it.
 *
 * Affordability is a small bipartite matching (atoms' slots × candidate permanents), solved here by
 * backtracking — the pools are a handful of permanents, so this stays cheap on the enumeration path.
 * The candidate pools come from [groups] — the one definition the enumerators, the validator, the
 * choice pauses and payment all read, so what is offered can't drift from what is accepted.
 */
object SacrificeCostAssignment {

    /** One sacrifice atom's demand: [count] distinct permanents out of [candidates]. */
    data class Group(val count: Int, val candidates: List<EntityId>)

    /** Every [CostAtom.Sacrifice] in [cost], top-level or inside a composite, in cost order. */
    fun atoms(cost: AbilityCost): List<CostAtom.Sacrifice> = when (cost) {
        is AbilityCost.Atom -> listOfNotNull(cost.atom as? CostAtom.Sacrifice)
        is AbilityCost.Composite -> cost.costs.flatMap { atoms(it) }
        else -> emptyList()
    }

    /**
     * One [Group] per sacrifice atom in [cost], in cost order. Each pool is the battlefield permanents
     * [playerId] controls (projected) that match the atom's filter — source-relative filters resolve
     * against [sourceId] — less those no cost may sacrifice ([SacrificeCostLegality]) and, for
     * "another", the source itself.
     */
    fun groups(
        state: GameState,
        cost: AbilityCost,
        playerId: EntityId,
        sourceId: EntityId?,
        predicateEvaluator: PredicateEvaluator,
    ): List<Group> {
        val atoms = atoms(cost)
        if (atoms.isEmpty()) return emptyList()
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId, sourceId = sourceId)
        val controlled = projected.getBattlefieldControlledBy(playerId).filter {
            state.getEntity(it)?.get<CardComponent>() != null && SacrificeCostLegality.canSacrificeForCost(state, it)
        }
        return atoms.map { atom ->
            Group(atom.count, controlled.filter { id ->
                !(atom.excludeSelf && id == sourceId) &&
                    predicateEvaluator.matches(state, projected, id, atom.filter, context)
            })
        }
    }

    /** True when [cost] holds two or more sacrifice atoms, so payment needs a joint assignment. */
    fun isJoint(cost: AbilityCost): Boolean = atoms(cost).size >= 2

    /**
     * Whether each group can be given its `count` distinct permanents with no permanent used twice
     * and none drawn from [used].
     */
    fun hasDisjointAssignment(groups: List<Group>, used: Set<EntityId> = emptySet()): Boolean {
        if (groups.any { g -> g.candidates.count { it !in used } < g.count }) return false
        return assign(groups, 0, groups.firstOrNull()?.count ?: 0, 0, used.toMutableSet())
    }

    /**
     * The permanents that may go to group [index] next, given the groups before it are already
     * paid with [chosen] (plus any picks for [index] itself already in [chosen]): a candidate is
     * offered only when the rest of this group and every later group can still be paid without it.
     * This is what keeps a player from spending the only Forest-that-is-also-a-Swamp on the Swamp
     * slot when no other Forest exists.
     */
    fun feasibleCandidates(groups: List<Group>, index: Int, chosen: Set<EntityId>, remainingInGroup: Int): List<EntityId> {
        val group = groups[index]
        val later = groups.drop(index + 1)
        return group.candidates.filter { c ->
            c !in chosen &&
                hasDisjointAssignment(listOf(Group(remainingInGroup - 1, group.candidates)) + later, chosen + c)
        }
    }

    /**
     * Split the flat [chosen] list into one segment per atom, by the atoms' counts in cost order.
     * A short list yields short (possibly empty) trailing segments.
     */
    fun segments(atoms: List<CostAtom.Sacrifice>, chosen: List<EntityId>): List<List<EntityId>> {
        var offset = 0
        return atoms.map { atom ->
            val segment = chosen.drop(offset).take(atom.count)
            offset += atom.count
            segment
        }
    }

    private fun assign(
        groups: List<Group>,
        groupIndex: Int,
        need: Int,
        start: Int,
        used: MutableSet<EntityId>,
    ): Boolean {
        if (groupIndex >= groups.size) return true
        if (need <= 0) {
            val next = groupIndex + 1
            return assign(groups, next, groups.getOrNull(next)?.count ?: 0, 0, used)
        }
        val candidates = groups[groupIndex].candidates
        // Within one group the picks are an unordered set: only try increasing indices.
        for (j in start until candidates.size) {
            val c = candidates[j]
            if (c in used) continue
            used += c
            if (assign(groups, groupIndex, need - 1, j + 1, used)) return true
            used -= c
        }
        return false
    }
}
