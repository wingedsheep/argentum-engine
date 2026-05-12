package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * CR 120.4: computes excess damage for a creature target.
 *
 * excess = max(0, amount − (toughness − prior_damage_marked))
 *
 * Returns 0 for non-creature targets (players, planeswalkers) where
 * projected toughness is unavailable.
 */
object ExcessDamageDetectionHandler {

    fun computeExcessDamage(state: GameState, targetId: EntityId, amount: Int): Int {
        val toughness = state.projectedState.getToughness(targetId) ?: return 0
        val priorDamage = state.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
        val remainingToughness = toughness - priorDamage
        return maxOf(0, amount - remainingToughness)
    }
}
