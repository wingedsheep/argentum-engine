package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles Saga chapter-ability mechanics (Rule 702.149 / Rule 714).
 *
 * Responsibilities wired from outside (keeping this object pure):
 *   - ETB lore counter  (Rule 702.149b) — [applyEtbLoreCounter]
 *   - After-draw-step lore counter (Rule 702.149c) — BeginningPhaseManager.addLoreCountersToSagas
 *   - Chapter trigger detection — TriggerDetector.detectSagaChapterTriggers
 *   - Sacrifice SBA (Rule 714.4) — SagaSacrificeCheck
 */
object SagaChapterAbilitiesHandler {

    /**
     * Apply the ETB lore-counter rule to a Saga that just entered the battlefield via
     * any zone-change path (including test helpers that bypass StackResolver).
     *
     * Mutates the state to add [SagaComponent] and one lore counter, then returns the
     * updated state together with the [CountersAddedEvent] that drives chapter-trigger
     * detection in [com.wingedsheep.engine.event.TriggerDetector].
     */
    fun applyEtbLoreCounter(
        state: GameState,
        entityId: EntityId,
        cardRegistry: CardRegistry
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(entityId) ?: return state to emptyList()
        val cardComponent = container.get<CardComponent>() ?: return state to emptyList()
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return state to emptyList()
        if (!cardDef.isSaga) return state to emptyList()

        val counters = container.get<CountersComponent>() ?: CountersComponent()
        val newState = state.updateEntity(entityId) { c ->
            c.with(SagaComponent(triggeredChapters = setOf(1)))
                .with(counters.withAdded(CounterType.LORE, 1))
        }

        val event = CountersAddedEvent(entityId, "LORE", 1, cardComponent.name)
        return newState to listOf(event)
    }
}
