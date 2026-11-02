package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.model.EntityId

/**
 * Whether a permanent may be sacrificed **to pay a cost of casting a spell or activating an
 * ability** (mana abilities included) — the one question every sacrifice-cost candidate pool,
 * validator and payment asks, so the answer can't drift between what is offered and what is accepted.
 *
 * Two projected flags say no:
 * - [AbilityFlag.CANT_BE_SACRIFICED] — the permanent can't be sacrificed at all, so no cost can
 *   sacrifice it either (Zurgo, Thunder's Decree's Warrior tokens during your end step).
 * - [AbilityFlag.CANT_BE_SACRIFICED_FOR_COSTS] — "Players can't sacrifice creatures to cast spells
 *   or activate abilities" (Angel of Jubilation). A cost that needs such a permanent can't be paid:
 *   "Sacrifice a creature" with only creatures, "Sacrifice this creature", emerge, casualty.
 *
 * Sacrifices a *resolving* spell or ability performs (an edict, exploit, "sacrifice it unless you
 * pay") ask only [canBeSacrificed] — they are neither casting nor activating.
 */
object SacrificeCostLegality {

    /**
     * Whether a permanent may be sacrificed **at all** — the question every sacrifice a resolving
     * spell or ability performs asks (edicts, "sacrifice N", a pipeline's sacrifice move, "sacrifice
     * it"). [AbilityFlag.CANT_BE_SACRIFICED] says no; a player told to sacrifice a permanent can't
     * choose one that can't be sacrificed, and a forced sacrifice of one simply doesn't happen.
     */
    fun canBeSacrificed(state: GameState, permanentId: EntityId): Boolean =
        !state.projectedState.hasKeyword(permanentId, AbilityFlag.CANT_BE_SACRIFICED)

    fun canSacrificeForCost(state: GameState, permanentId: EntityId): Boolean {
        val projected = state.projectedState
        return !projected.hasKeyword(permanentId, AbilityFlag.CANT_BE_SACRIFICED) &&
            !projected.hasKeyword(permanentId, AbilityFlag.CANT_BE_SACRIFICED_FOR_COSTS)
    }

    /** [candidates] narrowed to the permanents a cost may sacrifice. */
    fun sacrificeableForCost(state: GameState, candidates: List<EntityId>): List<EntityId> =
        candidates.filter { canSacrificeForCost(state, it) }
}
