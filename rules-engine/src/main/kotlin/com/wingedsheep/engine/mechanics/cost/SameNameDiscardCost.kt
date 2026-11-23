package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * The group rule behind [CostAtom.Discard.sameName] — "discard two nonland cards with the same
 * name" (Sphinx of the Chimes). One rule, read by every path an activated-ability discard cost
 * travels (affordability, offered candidates, payment validation), so they can't disagree. The
 * name twin of [SharedCreatureTypeTapCost].
 */
internal object SameNameDiscardCost {

    /**
     * Narrow [candidates] to the cards that can be part of a legal payment: those whose name is held
     * by at least [CostAtom.Discard.count] candidates. A `pool.size >= count` affordability check
     * stays correct on the narrowed pool. Cards of one name sit together, largest group first, so a
     * payer that takes the first `count` candidates (the AI) picks a legal set. A no-op unless the
     * axis is set.
     */
    fun eligible(state: GameState, atom: CostAtom.Discard, candidates: List<EntityId>): List<EntityId> {
        if (!atom.sameName) return candidates
        val byName = candidates.groupBy { name(state, it) }
        return byName.filter { (name, ids) -> name != null && ids.size >= atom.count }
            .values.sortedByDescending { it.size }.flatten()
    }

    /** Whether the chosen cards satisfy the axis: they all have the same name. */
    fun satisfiedBy(state: GameState, atom: CostAtom.Discard, chosen: Collection<EntityId>): Boolean {
        if (!atom.sameName) return true
        val names = chosen.map { name(state, it) }
        return names.none { it == null } && names.toSet().size <= 1
    }

    private fun name(state: GameState, id: EntityId): String? = state.getEntity(id)?.get<CardComponent>()?.name
}
