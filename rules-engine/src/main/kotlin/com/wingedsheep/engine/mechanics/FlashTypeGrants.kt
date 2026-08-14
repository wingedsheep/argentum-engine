package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Shared gate for [GrantFlashToSpellType.nthOfTypePerTurn].
 *
 * "The first creature spell you cast each turn … can be cast as though it had flash" (Radagast of
 * Rhosgobel) is a *timing* grant that only covers one spell per turn, so both flash read sites —
 * `CastPermissionUtils.canCastAtInstantSpeed` (which enumerates legal actions) and
 * `CastZoneResolver.hasGrantedFlash` (which authoritatively re-checks at cast time) — have to apply
 * the same count. They live in different packages and neither can see the other's private helpers,
 * so the count lives here rather than being written twice and drifting.
 *
 * The count comes off `GameState.spellsCastThisTurnByPlayer`, the same record
 * `CostGating.NthOfTypePerTurn` uses in `CostCalculator`, and with the same convention: the spell
 * being cast is **not** yet in the list, so the gate is open exactly while the caster has already
 * cast `n - 1` matching spells this turn. That means a matching spell already cast this turn closes
 * the window even if it was countered or fizzled — matching the "you cast" wording, which cares
 * about the cast and not the resolution.
 */
object FlashTypeGrants {

    /**
     * Whether [ability]'s per-turn gate (if any) currently lets [casterId] cast a matching spell at
     * instant speed. Always true for an ungated grant (Quick Sliver, Raff Capashen).
     */
    fun nthGateAllows(
        state: GameState,
        casterId: EntityId,
        ability: GrantFlashToSpellType,
        predicateEvaluator: PredicateEvaluator,
    ): Boolean {
        val n = ability.nthOfTypePerTurn ?: return true
        val castThisTurn = state.spellsCastThisTurnByPlayer[casterId] ?: emptyList()
        return castThisTurn.count { predicateEvaluator.matchesFilter(it, ability.filter) } == n - 1
    }
}
