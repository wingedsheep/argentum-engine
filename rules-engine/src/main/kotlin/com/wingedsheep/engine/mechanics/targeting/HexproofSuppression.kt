package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SuppressesHexproofForGroupComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Shared check for "creatures matching filter F can be targeted as though they didn't have
 * hexproof" effects (e.g. Nowhere to Run), and its player half — "your opponents … can be the
 * targets … as though they didn't have hexproof" (Kaya, Bane of the Dead), read by
 * [ControllerHexproof.appliesAgainst]. Consulted at three points:
 *  - cast-time legal-action enumeration ([com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils])
 *  - cast-time target validation ([TargetValidator])
 *  - resolution-time target legality ([com.wingedsheep.engine.mechanics.stack.StackResolver])
 *
 * Hexproof bypass is per-caster: only suppressors controlled by [casterId] grant the bypass.
 */
object HexproofSuppression {

    fun isSuppressedForCaster(
        state: GameState,
        projected: ProjectedState,
        targetId: EntityId,
        casterId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        return state.getBattlefield().any { suppressorId ->
            val suppressorController = projected.getController(suppressorId)
            if (suppressorController != casterId) return@any false
            val component = state.getEntity(suppressorId)
                ?.get<SuppressesHexproofForGroupComponent>() ?: return@any false
            val ctx = PredicateContext(controllerId = casterId, sourceId = suppressorId)
            component.filters.any { groupFilter ->
                when {
                    groupFilter.scope !is Scope.Battlefield -> false
                    groupFilter.excludeSelf && targetId == suppressorId -> false
                    else -> predicateEvaluator.matches(
                        state, projected, targetId, groupFilter.baseFilter, ctx
                    )
                }
            }
        }
    }

    /**
     * Whether a permanent controlled by [casterId] lets [casterId]'s spells and abilities target
     * [playerId] as though that player didn't have hexproof ([SuppressesHexproofForGroupComponent.players]).
     */
    fun isPlayerSuppressedForCaster(
        state: GameState,
        playerId: EntityId,
        casterId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Boolean {
        val projected = state.projectedState
        return state.getBattlefield().any { suppressorId ->
            if (projected.getController(suppressorId) != casterId) return@any false
            val players = state.getEntity(suppressorId)
                ?.get<SuppressesHexproofForGroupComponent>()?.players
            if (players.isNullOrEmpty()) return@any false
            val ctx = PredicateContext(controllerId = casterId, sourceId = suppressorId)
            players.any { predicateEvaluator.matchesPlayer(state, projected, it, playerId, ctx) }
        }
    }
}
