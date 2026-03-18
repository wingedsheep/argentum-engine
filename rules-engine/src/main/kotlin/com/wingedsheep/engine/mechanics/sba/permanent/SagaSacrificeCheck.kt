package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.SbaZoneMovementHelper
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType

/**
 * 714.4 - If the number of lore counters on a Saga permanent is greater than or equal
 * to its final chapter number, and it isn't the source of a chapter ability that has
 * triggered but not yet left the stack, the Saga's controller sacrifices it.
 */
class SagaSacrificeCheck(
    private val cardRegistry: CardRegistry?
) : StateBasedActionCheck {
    override val name = "714.4 Saga Sacrifice"
    override val order = SbaOrder.SAGA_SACRIFICE

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            container.get<SagaComponent>() ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val registry = cardRegistry ?: continue
            val cardDef = registry.getCard(cardComponent.cardDefinitionId) ?: continue
            val finalChapter = cardDef.finalChapter ?: continue

            val loreCount = counters.getCount(CounterType.LORE)
            if (loreCount < finalChapter) continue

            // Check if any chapter ability from this saga is on the stack
            val hasChapterOnStack = newState.stack.any { stackId ->
                val stackEntity = newState.getEntity(stackId) ?: return@any false
                val triggeredComponent = stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()
                triggeredComponent?.sourceId == entityId
            }
            if (hasChapterOnStack) continue

            val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                newState, entityId, cardComponent
            )
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
