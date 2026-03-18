package com.wingedsheep.engine.mechanics.sba.creature

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * 704.5f - A creature with toughness 0 or less is put into its owner's graveyard.
 */
class ZeroToughnessCheck : StateBasedActionCheck {
    override val name = "704.5f Zero Toughness"
    override val order = SbaOrder.ZERO_TOUGHNESS

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        val projected = state.projectedState

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!projected.isCreature(entityId)) continue

            val effectiveToughness = projected.getToughness(entityId) ?: 0

            if (effectiveToughness <= 0) {
                val result = SbaZoneMovementHelper.putCreatureInGraveyard(
                    newState, entityId, cardComponent, "zero toughness"
                )
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
