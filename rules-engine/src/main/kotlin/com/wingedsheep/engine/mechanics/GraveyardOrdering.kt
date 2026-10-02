package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/** Preserve existing cards and let each owner order only the simultaneous arrivals, top first. */
object GraveyardOrdering {
    fun finish(result: EffectResult): EffectResult {
        if (result.outcome !is Outcome.Done || !result.events.any { it is ZoneChangeEvent && it.toZone == Zone.GRAVEYARD && !it.graveyardOrderFinalized }) return result
        if (!result.state.preserveGraveyardOrder) return result
        val events = result.events.map { if (it is ZoneChangeEvent && it.toZone == Zone.GRAVEYARD) it.copy(graveyardOrderFinalized = true) else it }
        val groups = result.events.filterIsInstance<ZoneChangeEvent>()
            .filter { it.toZone == Zone.GRAVEYARD && !it.graveyardOrderFinalized && it.entityId in result.state.getGraveyard(it.ownerId) && result.state.getEntity(it.entityId)?.has<TokenComponent>() != true }
            .groupBy { it.ownerId }.mapValues { (_, moves) -> moves.map { it.entityId }.distinct() }.filterValues { it.size > 1 }
        if (groups.isEmpty()) return result.copy(events = events)
        val paused = ask(result.state, groups, events, result.updatedCollections, result.updatedStoredNumbers, result.updatedChosenValues, result.updatedSubtypeGroups, result.updatedSacrificedPermanents)
        return result.copy(state = paused.state, events = paused.events, outcome = paused.outcome)
    }

    fun finish(result: ExecutionResult): ExecutionResult =
        finish(EffectResult.from(result)).toExecutionResult()

    fun ask(state: GameState, groups: Map<EntityId, List<EntityId>>, events: List<GameEvent>,
            collections: Map<String, List<EntityId>> = emptyMap(),
            numbers: Map<String, Int> = emptyMap(),
            chosenValues: Map<String, String> = emptyMap(),
            subtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
            sacrificed: List<com.wingedsheep.engine.state.components.stack.EntitySnapshot> = emptyList()): ExecutionResult {
        if (groups.isEmpty()) return ExecutionResult.success(state, events)
        val active = state.turnOrder.indexOf(state.activePlayerId).coerceAtLeast(0)
        val order = state.turnOrder.drop(active) + state.turnOrder.take(active)
        val owner = order.first { it in groups }
        val cards = groups.getValue(owner)
        return state.suspendForDecision(
            { id -> OrderObjectsDecision(id, owner, "Order the cards entering your graveyard (top card first)",
                DecisionContext(phase = DecisionPhase.RESOLUTION), cards,
                cards.associateWith { cardId ->
                    val c = state.getEntity(cardId)?.get<CardComponent>()
                    SearchCardInfo(name = c?.name ?: "Card", manaCost = c?.manaCost?.toString() ?: "",
                        typeLine = c?.typeLine?.toString() ?: "", imageUri = c?.imageUri)
                }, orderingTitle = "Order Graveyard", firstLabel = "TOP OF GRAVEYARD", lastLabel = "BOTTOM OF NEW CARDS") },
            GraveyardOrderContinuation(owner, cards, groups - owner, events, collections, numbers, chosenValues, subtypeGroups, sacrificed)
        )
    }

    fun reorder(state: GameState, owner: EntityId, cards: List<EntityId>, topFirst: List<EntityId>): GameState {
        val zone = ZoneKey(owner, Zone.GRAVEYARD)
        val existing = state.getZone(zone)
        // Graveyards store oldest first, unlike a library whose first card is its top.
        return state.reorderZone(zone, existing.filter { it !in cards } + topFirst.asReversed())
    }
}
