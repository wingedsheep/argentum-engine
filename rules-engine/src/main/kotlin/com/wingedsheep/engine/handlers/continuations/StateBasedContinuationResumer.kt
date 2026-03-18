package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent

class StateBasedContinuationResumer(
    private val ctx: ContinuationContext
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(LegendRuleContinuation::class, ::resumeLegendRule)
    )

    private fun resumeLegendRule(
        state: GameState,
        continuation: LegendRuleContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for legend rule")
        }

        if (response.selectedCards.size != 1) {
            return ExecutionResult.error(state, "Must select exactly one legendary permanent to keep")
        }

        val keepEntityId = response.selectedCards.first()

        // Validate the selection is one of the duplicates
        if (keepEntityId !in continuation.allDuplicates) {
            return ExecutionResult.error(state, "Selected permanent is not one of the legendary duplicates")
        }

        // Put all other duplicates into graveyard
        val toRemove = continuation.allDuplicates.filter { it != keepEntityId }

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in toRemove) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val result = SbaZoneMovementHelper.putPermanentInGraveyard(newState, entityId, cardComponent)
            newState = result.newState
            events.addAll(result.events)
        }

        return checkForMore(newState, events)
    }
}
