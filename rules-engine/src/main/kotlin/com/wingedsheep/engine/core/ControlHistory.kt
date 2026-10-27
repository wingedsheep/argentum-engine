package com.wingedsheep.engine.core

import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.TurnStartControl
import com.wingedsheep.engine.state.UpkeepControl
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.predicates.ControlMark
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/** A turn-start snapshot only loses entries: losing control and regaining it cannot restore continuity. */
object ControlHistory {
    fun beginTurn(state: GameState): GameState =
        state.copy(controlAtTurnStart = snapshot(state), untappedAtTurnStart = untappedSnapshot(state))

    fun initialize(state: GameState): GameState {
        val control = state.controlAtTurnStart ?: snapshot(state, imported = true)
        val untapped = state.untappedAtTurnStart ?: untappedSnapshot(state, imported = true)
        return if (control === state.controlAtTurnStart && untapped === state.untappedAtTurnStart) state
        else state.copy(controlAtTurnStart = control, untappedAtTurnStart = untapped)
    }

    /**
     * The objects on the battlefield untapped as the turn begins — before the untap step, so
     * untapping during it doesn't count. Phased-out permanents are treated as though they don't
     * exist (CR 702.26b), so they didn't start the turn untapped. Unlike the control snapshot this
     * never loses entries: tapping later, or changing control, doesn't change how the turn started.
     */
    private fun untappedSnapshot(state: GameState, imported: Boolean = false): Set<ObjectRef> =
        state.getBattlefield().mapNotNullTo(HashSet()) { id ->
            val container = state.getEntity(id) ?: return@mapNotNullTo null
            if (container.has<TappedComponent>() || container.has<PhasedOutComponent>() ||
                (imported && container.has<EnteredThisTurnComponent>())) return@mapNotNullTo null
            state.objectRef(id)
        }

    /** `StatePredicate.StartedTurnUntapped`: this very object was untapped on the battlefield as the turn began. */
    fun startedTurnUntapped(state: GameState, id: EntityId): Boolean {
        if (state.logicalZone(id)?.zoneType != Zone.BATTLEFIELD) return false
        val ref = state.objectRef(id) ?: return false
        return state.untappedAtTurnStart?.contains(ref) ?: return false
    }

    private fun snapshot(state: GameState, imported: Boolean = false): Map<EntityId, TurnStartControl> {
        val projected = state.projectedState
        return state.objectIdentities.mapNotNull { (id, identity) ->
            if (identity.logicalZone.zoneType != Zone.BATTLEFIELD ||
                (imported && state.getEntity(id)?.has<EnteredThisTurnComponent>() == true)) return@mapNotNull null
            val controller = controller(state, projected, id) ?: return@mapNotNull null
            id to TurnStartControl(controller, state.objectRef(id) ?: return@mapNotNull null)
        }.toMap()
    }

    /** `StatePredicate.ControlledContinuouslySince`: dispatches on the [mark] measured from. */
    fun controlledSince(state: GameState, projected: ProjectedState, id: EntityId, mark: ControlMark): Boolean =
        controlledSince(state, id, controller(state, projected, id), mark)

    fun controlledSince(state: GameState, id: EntityId, currentController: EntityId?, mark: ControlMark): Boolean =
        when (mark) {
            ControlMark.TURN_BEGAN -> matches(state, id, currentController)
            ControlMark.YOUR_LAST_UPKEEP -> controlledSinceLastUpkeep(state, id, currentController)
        }

    fun matches(state: GameState, id: EntityId, currentController: EntityId?): Boolean {
        if (state.logicalZone(id)?.zoneType != Zone.BATTLEFIELD) return false
        val history = state.controlAtTurnStart
            ?: return state.getEntity(id)?.has<EnteredThisTurnComponent>() != true
        val start = history[id] ?: return false
        return state.isCurrentObject(start.objectRef) && currentController == start.controllerId
    }

    /**
     * The beginning of an upkeep step — the turn's own or an additional one — for every player
     * whose upkeep it is: each one's "since your last upkeep" snapshot rotates, and the new one
     * holds what they control right now. Called before upkeep triggers are detected.
     */
    fun beginUpkeep(state: GameState, players: List<EntityId>): GameState {
        val projected = state.projectedState
        val updated = state.controlAtUpkeepStart.toMutableMap()
        for (player in players) {
            // Phased-out permanents included: phasing doesn't interrupt control (CR 702.26d).
            val now = state.allBattlefieldEntities().mapNotNull { id ->
                if (controller(state, projected, id) != player) return@mapNotNull null
                id to (state.objectRef(id) ?: return@mapNotNull null)
            }.toMap()
            updated[player] = UpkeepControl(current = now, previous = updated[player]?.current)
        }
        return state.copy(controlAtUpkeepStart = updated)
    }

    /**
     * The end of an upkeep step: only that player's upkeep reads the previous snapshot, so it is
     * dropped rather than kept current by [record] until their next upkeep.
     */
    fun endUpkeep(state: GameState, players: List<EntityId>): GameState {
        val upkeeps = state.controlAtUpkeepStart
        if (players.none { upkeeps[it]?.previous != null }) return state
        return state.copy(controlAtUpkeepStart = upkeeps.mapValues { (player, record) ->
            if (player in players && record.previous != null) record.copy(previous = null) else record
        })
    }

    /**
     * `ControlledContinuouslySince(ControlMark.YOUR_LAST_UPKEEP)`: its controller has controlled this
     * very object without interruption since the beginning of their last upkeep. During that player's
     * own upkeep the "last" upkeep is the one before it; a player with no such upkeep has controlled
     * nothing since one, so everything they control reads as having come under their control since.
     */
    private fun controlledSinceLastUpkeep(state: GameState, id: EntityId, currentController: EntityId?): Boolean {
        if (currentController == null || state.logicalZone(id)?.zoneType != Zone.BATTLEFIELD) return false
        val record = state.controlAtUpkeepStart[currentController] ?: return false
        val active = state.activePlayerId
        val inOwnUpkeep = state.step == Step.UPKEEP && active != null &&
            currentController in state.sharedTurnTeam(active)
        val since = (if (inOwnUpkeep) record.previous else record.current) ?: return false
        val ref = since[id] ?: return false
        return state.isCurrentObject(ref)
    }

    fun record(state: GameState, events: List<GameEvent>): GameState {
        val history = state.controlAtTurnStart
        val upkeeps = state.controlAtUpkeepStart
        if (history.isNullOrEmpty() && upkeeps.isEmpty()) return state
        val interrupted = events.filterIsInstance<ControlChangedEvent>()
            .filter { it.oldControllerId != it.newControllerId }.mapTo(HashSet()) { it.permanentId }
        // Only control-layer effects can make a controller differ from its stored component.
        // Avoid a full battlefield projection after every instruction on ordinary boards; any
        // potential control effect, including inactive or phased sources, keeps the projected path.
        val projected = if (hasControlEffects(state)) state.projectedState else null
        var result = state
        if (!history.isNullOrEmpty()) {
            val retained = history.filter { (id, _) ->
                id !in interrupted && matches(state, id, controller(state, projected, id))
            }
            if (retained.size != history.size) result = result.copy(controlAtTurnStart = retained)
        }
        if (upkeeps.isNotEmpty()) {
            var changed = false
            val retained = upkeeps.mapValues { (player, record) ->
                val current = retainUpkeep(state, projected, player, record.current, interrupted)
                val previous = record.previous?.let { retainUpkeep(state, projected, player, it, interrupted) }
                if (current === record.current && previous === record.previous) record
                else UpkeepControl(current, previous).also { changed = true }
            }
            if (changed) result = result.copy(controlAtUpkeepStart = retained)
        }
        return result
    }

    private fun retainUpkeep(
        state: GameState,
        projected: ProjectedState?,
        player: EntityId,
        since: Map<EntityId, ObjectRef>,
        interrupted: Set<EntityId>,
    ): Map<EntityId, ObjectRef> {
        val retained = since.filter { (id, ref) ->
            id !in interrupted && state.isCurrentObject(ref) && controller(state, projected, id) == player
        }
        return if (retained.size == since.size) since else retained
    }

    // Mirrors the static and floating sources read by StateProjector.collectContinuousEffects.
    private fun hasControlEffects(state: GameState): Boolean =
        state.floatingEffects.any { it.effect.layer == Layer.CONTROL } ||
            state.zones.any { (zone, ids) ->
                zone.zoneType == Zone.BATTLEFIELD && ids.any { id ->
                    state.getEntity(id)?.get<ContinuousEffectSourceComponent>()
                        ?.effects?.any { it.layer == Layer.CONTROL } == true
                }
            }

    // Phased-out permanents have no projection entry, but their battlefield visit continues.
    private fun controller(state: GameState, projected: ProjectedState?, id: EntityId): EntityId? =
        projected?.getController(id) ?: state.getEntity(id)?.get<PhasedOutComponent>()?.phasedOutByController
            ?: state.getEntity(id)?.get<ControllerComponent>()?.playerId
}
