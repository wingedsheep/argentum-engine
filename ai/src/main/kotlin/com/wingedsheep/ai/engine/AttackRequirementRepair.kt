package com.wingedsheep.ai.engine

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.OpponentsMustAttackYouRequirement
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Makes a planned attack obey "each opponent must attack you … with at least one creature"
 * ([com.wingedsheep.sdk.scripting.OpponentsMustAttackYou], Trove of Temptation).
 *
 * That requirement names no creature, so it never arrives as `mandatoryAttackers`; a plan built
 * from the combat heuristics (or the empty "don't attack" plan) is rejected by the engine. For each
 * required player the plan doesn't already attack, this sends the cheapest spare attacker at them —
 * or, with no spare, redirects the cheapest planned one — keeping the rest of the plan.
 */
object AttackRequirementRepair {

    /** [candidates] in the order to try them for one required player: lowest power first. */
    fun candidateOrder(state: GameState, candidates: Collection<EntityId>): List<EntityId> {
        val projected = state.projectedState
        return candidates.sortedBy { projected.getPower(it) ?: 0 }
    }

    /** The players [playerId] must attack this combat, empty in the overwhelmingly common case. */
    fun requiredPlayers(state: GameState, cardRegistry: CardRegistry?, playerId: EntityId): List<EntityId> {
        if (cardRegistry == null) return emptyList()
        return OpponentsMustAttackYouRequirement.requiringPlayers(
            state, cardRegistry, PredicateEvaluator(cardRegistry), playerId
        )
    }

    fun repair(
        state: GameState,
        cardRegistry: CardRegistry?,
        playerId: EntityId,
        plan: Map<EntityId, EntityId>,
        validAttackers: List<EntityId>,
        mandatory: Set<EntityId> = emptySet(),
    ): Map<EntityId, EntityId> {
        val required = requiredPlayers(state, cardRegistry, playerId)
        if (required.isEmpty()) return plan
        val projected = state.projectedState
        val repaired = plan.toMutableMap()
        for (player in required) {
            if (repaired.values.any { OpponentsMustAttackYouRequirement.isAttackOn(state, projected, it, player) }) {
                continue
            }
            val spare = candidateOrder(state, validAttackers.filter { it !in repaired })
            // Never redirect an attacker that is already what satisfies another required player.
            val redirectable = candidateOrder(state, repaired.filter { (attacker, defender) ->
                attacker !in mandatory &&
                    required.none { OpponentsMustAttackYouRequirement.isAttackOn(state, projected, defender, it) }
            }.keys)
            val pick = spare.firstOrNull() ?: redirectable.firstOrNull() ?: continue
            repaired[pick] = player
        }
        return repaired
    }
}
