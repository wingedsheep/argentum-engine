package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CountersRemovedFromYourPermanentsThisTurnComponent
import com.wingedsheep.sdk.core.Zone

/**
 * Turn history of counters leaving permanents, recorded from events rather than at each removal.
 *
 * Counters leave permanents along more than twenty paths — costs, effects, moves, +1/+1 and -1/-1
 * annihilation, shield and stun counters — and every one of them emits a [CountersRemovedEvent]. The
 * [Settler] sees every event, so recording here covers them all, including a path added later.
 */
object CounterHistory {

    /**
     * Credit each counter kind removed in [events] to the controller of the permanent it left — "an
     * oil counter was removed from a permanent you controlled this turn" (Churning Reservoir).
     *
     * The controller is the projected one when the permanent is still on the battlefield. When the
     * same action also moved it off the battlefield (removed as a cost, then sacrificed), it is the
     * last-known controller its [ZoneChangeEvent] carries. Anything else — a player, a suspended card
     * in exile — isn't a permanent and records nothing.
     */
    fun recordRemovals(state: GameState, events: List<GameEvent>): GameState {
        var result = state
        for (event in events) {
            if (event !is CountersRemovedEvent || event.amount <= 0) continue
            val controllerId = controllerOf(state, event, events) ?: continue
            result = result.updateEntity(controllerId) { container ->
                val existing = container.get<CountersRemovedFromYourPermanentsThisTurnComponent>()
                    ?: CountersRemovedFromYourPermanentsThisTurnComponent()
                container.with(existing.with(event.counterType))
            }
        }
        return result
    }

    private fun controllerOf(
        state: GameState,
        event: CountersRemovedEvent,
        events: List<GameEvent>
    ): com.wingedsheep.sdk.model.EntityId? {
        if (event.entityId in state.getBattlefield()) return state.projectedState.getController(event.entityId)
        val departure = events.firstOrNull {
            it is ZoneChangeEvent && it.entityId == event.entityId && it.fromZone == Zone.BATTLEFIELD
        } as ZoneChangeEvent?
        return departure?.lastKnown?.controllerId
    }
}
