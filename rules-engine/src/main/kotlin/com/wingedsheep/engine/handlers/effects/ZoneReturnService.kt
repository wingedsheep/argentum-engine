package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.ZoneTransitionCause
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.Zone

/** Resolves zone-return one-shot effects before the next instruction or priority window. */
object ZoneReturnService {
    fun returnDepartedSources(zones: ZoneTransitionService, state: GameState): ZoneTransitionResult {
        if (state.zoneReturns.isEmpty()) return ZoneTransitionResult(state, emptyList())
        val due = state.zoneReturns.filter { !state.isCurrentObject(it.source) }
        val remaining = state.zoneReturns.filter {
            state.isCurrentObject(it.source) && state.isCurrentObject(it.movedObject)
        }
        if (due.isEmpty() && remaining.size == state.zoneReturns.size) {
            return ZoneTransitionResult(state, emptyList())
        }
        // Consume first: returning an object can itself cause another zone transition.
        var newState = state.copy(zoneReturns = remaining)
        val events = mutableListOf<GameEvent>()
        val transitions = mutableListOf<ZoneTransitionOutcome>()
        for (entry in due) {
            val id = entry.movedObject.entityId
            if (!newState.isCurrentObject(entry.movedObject)) continue
            val container = newState.getEntity(id) ?: continue
            if (container.has<TokenComponent>()) continue
            val owner = container.get<CardComponent>()?.ownerId ?: continue
            val origin = newState.logicalZone(id)
            val result = zones.moveToZone(
                newState, id, entry.previousZone, ZoneEntryOptions(controllerId = owner)
            )
            newState = result.state
            events.addAll(result.events.map {
                if (it is ZoneChangeEvent) it.copy(transitionCause = ZoneTransitionCause.DURATION_RETURN) else it
            })
            transitions.addAll(result.transitions.map { it.copy(cause = ZoneTransitionCause.DURATION_RETURN) })
            if (result.actualDestination == Zone.BATTLEFIELD) {
                val (entered, entryEvents) = EntersWithReplacements.applyOnEntry(
                    newState, id, owner, zones.cardRegistry,
                    predicateEvaluator = zones.predicateEvaluator, preEntryZone = origin
                )
                newState = entered
                events.addAll(entryEvents)
            }
        }
        return ZoneTransitionResult(newState, events, transitions = transitions)
    }
}
