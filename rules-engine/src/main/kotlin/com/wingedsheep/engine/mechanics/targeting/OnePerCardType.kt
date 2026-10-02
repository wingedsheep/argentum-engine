package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.model.EntityId

/**
 * The "at most one of each card type" cross-target rule behind
 * [com.wingedsheep.sdk.scripting.targets.TargetObject.onePerCardType] ("up to one target nonland
 * card of each card type", Uldaros Theorix).
 *
 * It is a single instance of the word "target", so each object is chosen once (CR 115.3) and fills
 * exactly one card-type slot. A selection is legal when every chosen object can be paired with a
 * *distinct* card type it has — a bipartite matching, not "no two share a type": an artifact
 * creature and a creature are legal together (artifact + creature), two plain creatures are not.
 * Solved with Kuhn's augmenting paths; the slot count is bounded by the handful of card types, so
 * the search is trivially small.
 */
object OnePerCardType {

    private val CARD_TYPE_NAMES: Set<String> = CardType.entries.mapTo(mutableSetOf()) { it.name }

    /** Whether [ids] can each be assigned a different card type they have. */
    fun isSatisfied(state: GameState, ids: List<EntityId>): Boolean =
        canAssignDistinct(ids.map { cardTypesOf(state, it) })

    /**
     * An object's card types (CR 205.2a) — projected on the battlefield so a type-changing effect
     * counts, the card's own type line elsewhere. Supertypes and subtypes are excluded.
     */
    fun cardTypesOf(state: GameState, id: EntityId): Set<String> {
        if (id in state.getBattlefield()) {
            val projected = state.projectedState.getTypes(id).filterTo(mutableSetOf()) { it in CARD_TYPE_NAMES }
            if (projected.isNotEmpty()) return projected
        }
        return state.getEntity(id)?.get<CardComponent>()?.typeLine?.cardTypes
            ?.mapTo(mutableSetOf()) { it.name }
            ?: emptySet()
    }

    /** True when each set in [typeSets] can be matched to a distinct element it contains. */
    fun canAssignDistinct(typeSets: List<Set<String>>): Boolean =
        maxDistinctAssignment(typeSets) == typeSets.size

    /**
     * The most sets in [typeSets] that can each be matched to a distinct element they contain —
     * the size of a maximum bipartite matching. It is the true cap on a "one of each card type"
     * selection: three artifact creatures have two types between them but fill only two slots.
     */
    fun maxDistinctAssignment(typeSets: List<Set<String>>): Int {
        val owner = mutableMapOf<String, Int>()
        fun augment(index: Int, visited: MutableSet<String>): Boolean {
            for (type in typeSets[index]) {
                if (!visited.add(type)) continue
                val holder = owner[type]
                if (holder == null || augment(holder, visited)) {
                    owner[type] = index
                    return true
                }
            }
            return false
        }
        return typeSets.indices.count { augment(it, mutableSetOf()) }
    }
}
