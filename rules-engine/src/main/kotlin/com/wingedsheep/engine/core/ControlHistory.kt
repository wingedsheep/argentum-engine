package com.wingedsheep.engine.core

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.TurnStartControl
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/** A turn-start snapshot only loses entries: losing control and regaining it cannot restore continuity. */
object ControlHistory {
    fun beginTurn(state: GameState): GameState = state.copy(controlAtTurnStart = snapshot(state))

    fun initialize(state: GameState): GameState =
        if (state.controlAtTurnStart != null) state else state.copy(controlAtTurnStart = snapshot(state, imported = true))

    private fun snapshot(state: GameState, imported: Boolean = false): Map<EntityId, TurnStartControl> {
        val projected = state.projectedState
        return state.objectIdentities.mapNotNull { (id, identity) ->
            if (identity.logicalZone.zoneType != Zone.BATTLEFIELD ||
                (imported && state.getEntity(id)?.has<EnteredThisTurnComponent>() == true)) return@mapNotNull null
            val controller = controller(state, projected, id) ?: return@mapNotNull null
            id to TurnStartControl(controller, state.objectRef(id) ?: return@mapNotNull null)
        }.toMap()
    }

    fun matches(state: GameState, projected: ProjectedState, id: EntityId): Boolean {
        return matches(state, id, controller(state, projected, id))
    }

    fun matches(state: GameState, id: EntityId, currentController: EntityId?): Boolean {
        if (state.logicalZone(id)?.zoneType != Zone.BATTLEFIELD) return false
        val history = state.controlAtTurnStart
            ?: return state.getEntity(id)?.has<EnteredThisTurnComponent>() != true
        val start = history[id] ?: return false
        return state.isCurrentObject(start.objectRef) && currentController == start.controllerId
    }

    fun record(state: GameState, events: List<GameEvent>): GameState {
        val history = state.controlAtTurnStart ?: return state
        if (history.isEmpty()) return state
        val interrupted = events.filterIsInstance<ControlChangedEvent>()
            .filter { it.oldControllerId != it.newControllerId }.mapTo(HashSet()) { it.permanentId }
        val projected = state.projectedState
        val retained = history.filter { (id, _) -> id !in interrupted && matches(state, projected, id) }
        return if (retained.size == history.size) state else state.copy(controlAtTurnStart = retained)
    }

    // Phased-out permanents have no projection entry, but their battlefield visit continues.
    private fun controller(state: GameState, projected: ProjectedState, id: EntityId): EntityId? =
        projected.getController(id) ?: state.getEntity(id)?.get<PhasedOutComponent>()?.phasedOutByController
            ?: state.getEntity(id)?.get<ControllerComponent>()?.playerId
}
