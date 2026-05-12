package com.wingedsheep.engine.handlers.filters

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter

class UntappedCreaturesAndOrTreasuresFilter {

    private val predicateEvaluator = PredicateEvaluator()

    private val filter = (GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype(Subtype("Treasure")))
        .untapped()
        .youControl()

    fun findMatching(state: GameState, controllerId: EntityId): List<EntityId> {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = controllerId)
        return state.getBattlefield().filter { entityId ->
            predicateEvaluator.matchesWithProjection(state, projected, entityId, filter, context)
        }
    }
}
