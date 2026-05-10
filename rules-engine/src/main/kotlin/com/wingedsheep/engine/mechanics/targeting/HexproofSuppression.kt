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
 * hexproof" effects (e.g. Nowhere to Run). Consulted at three points:
 *  - cast-time legal-action enumeration ([com.wingedsheep.engine.legalactions.utils.TargetEnumerationUtils])
 *  - cast-time target validation ([TargetValidator])
 *  - resolution-time target legality ([com.wingedsheep.engine.mechanics.stack.StackResolver])
 *
 * Hexproof bypass is per-caster: only suppressors controlled by [casterId] grant the bypass.
 */
object HexproofSuppression {
    private val predicateEvaluator = PredicateEvaluator()

    fun isSuppressedForCaster(
        state: GameState,
        projected: ProjectedState,
        targetId: EntityId,
        casterId: EntityId
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
                    else -> predicateEvaluator.matchesWithProjection(
                        state, projected, targetId, groupFilter.baseFilter, ctx
                    )
                }
            }
        }
    }
}
