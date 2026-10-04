package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CountersRemovedFromYourPermanentsThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerCountersRemovedThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlusOneCountersPutOnYourCreaturesThisTurnComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone

/**
 * Turn history of counters arriving on and leaving permanents, recorded from events rather than at
 * each placement or removal.
 *
 * Counters leave permanents along more than twenty paths — costs, effects, moves, +1/+1 and -1/-1
 * annihilation, shield and stun counters — and every one of them emits a [CountersRemovedEvent];
 * placements likewise all emit a [CountersAddedEvent]. The [Settler] sees every event, so recording
 * here covers them all, including a path added later.
 */
object CounterHistory {

    /** Both directions of [events]' counter history: [recordPlacements] then [recordRemovals]. */
    fun record(state: GameState, events: List<GameEvent>): GameState =
        recordRemovals(recordPlacements(state, events), events)

    /**
     * Tally the +1/+1 counters each player put on creatures they controlled — "for each +1/+1
     * counter you've put on creatures under your control this turn" (Iridescent Hornbeetle).
     *
     * The placer is the event's [CountersAddedEvent.placedBy] (CR 122.6a: a permanent entering with
     * counters has them put on it by its controller); a placement with no attributed placer counts
     * for no one. The creature has to be the placer's: its projected controller and type when it is
     * still on the battlefield, or — when the same action already moved it off — the last-known ones
     * its [ZoneChangeEvent] carries.
     */
    fun recordPlacements(state: GameState, events: List<GameEvent>): GameState {
        var result = state
        for (event in events) {
            if (event !is CountersAddedEvent || event.amount <= 0) continue
            if (event.counterType != CounterType.PLUS_ONE_PLUS_ONE) continue
            val placerId = event.placedBy ?: continue
            if (!isCreatureControlledBy(state, event.entityId, placerId, events)) continue
            result = result.updateEntity(placerId) { container ->
                val existing = container.get<PlusOneCountersPutOnYourCreaturesThisTurnComponent>()
                    ?: PlusOneCountersPutOnYourCreaturesThisTurnComponent()
                container.with(existing.copy(count = existing.count + event.amount))
            }
        }
        return result
    }

    private fun isCreatureControlledBy(
        state: GameState,
        entityId: com.wingedsheep.sdk.model.EntityId,
        playerId: com.wingedsheep.sdk.model.EntityId,
        events: List<GameEvent>
    ): Boolean {
        if (entityId in state.getBattlefield()) {
            val projected = state.projectedState
            return projected.isCreature(entityId) && projected.getController(entityId) == playerId
        }
        val departure = events.firstOrNull {
            it is ZoneChangeEvent && it.entityId == entityId && it.fromZone == Zone.BATTLEFIELD
        } as ZoneChangeEvent? ?: return false
        val lastKnown = departure.lastKnown ?: return false
        return lastKnown.typeLine?.isCreature == true && lastKnown.controllerId == playerId
    }

    /**
     * Credit each counter kind removed in [events] to the controller of the permanent it left — "an
     * oil counter was removed from a permanent you controlled this turn" (Churning Reservoir).
     *
     * The controller is the projected one when the permanent is still on the battlefield. When the
     * same action also moved it off the battlefield (removed as a cost, then sacrificed), it is the
     * last-known controller its [ZoneChangeEvent] carries. Anything else — a player, a suspended card
     * in exile — isn't a permanent and records nothing here.
     *
     * Counters removed from a player themself are tallied by amount on that player instead — "if
     * you've paid or lost four or more {E} this turn" (Izzet Generatorium).
     */
    fun recordRemovals(state: GameState, events: List<GameEvent>): GameState {
        var result = state
        for (event in events) {
            if (event !is CountersRemovedEvent || event.amount <= 0) continue
            if (event.entityId in state.turnOrder) {
                result = result.updateEntity(event.entityId) { container ->
                    val existing = container.get<PlayerCountersRemovedThisTurnComponent>()
                        ?: PlayerCountersRemovedThisTurnComponent()
                    container.with(existing.with(event.counterType, event.amount))
                }
                continue
            }
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
