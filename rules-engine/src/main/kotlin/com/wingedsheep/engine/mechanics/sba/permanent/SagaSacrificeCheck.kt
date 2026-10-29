package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
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
 *
 * It is a sacrifice (CR 701.21), so it emits [PermanentsSacrificedEvent] and counts toward
 * "sacrificed this turn" — "whenever you sacrifice an enchantment" (Narci, Fable Singer) sees it.
 */
class SagaSacrificeCheck(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry
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

            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val finalChapter = cardDef.finalChapter ?: continue

            val loreCount = counters.getCount(CounterType.LORE)
            if (loreCount < finalChapter) continue

            // "Triggered but not yet left the stack" covers a chapter ability on the stack and one
            // still waiting to be put there: the settle boundary checks state-based actions before
            // it places the chapter that lore accrual just triggered.
            val hasChapterOnStack = newState.stack.any { stackId ->
                val stackEntity = newState.getEntity(stackId) ?: return@any false
                val triggeredComponent = stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()
                triggeredComponent?.sourceId == entityId
            }
            val hasChapterWaiting = newState.waitingTriggers.any { it.sourceId == entityId }
            if (hasChapterOnStack || hasChapterWaiting) continue

            (newState.projectedState.getController(entityId) ?: cardComponent.ownerId)?.let { controllerId ->
                newState = ZoneTransitionService.trackPermanentSacrifice(newState, listOf(entityId), controllerId)
                events.add(PermanentsSacrificedEvent(controllerId, listOf(entityId), listOf(cardComponent.name)))
            }

            val result = SbaZoneMovementHelper.putPermanentInGraveyard(
                zones,
                newState, entityId, cardComponent
            )
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }
}
