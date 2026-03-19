package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Converts an entity ID to the appropriate [ChosenTarget] subtype
 * based on the entity's current zone.
 */
fun entityIdToChosenTarget(state: GameState, entityId: EntityId): ChosenTarget {
    return when {
        entityId in state.turnOrder -> ChosenTarget.Player(entityId)
        entityId in state.getBattlefield() -> ChosenTarget.Permanent(entityId)
        entityId in state.stack -> ChosenTarget.Spell(entityId)
        else -> {
            val graveyardOwner = state.turnOrder.find { playerId ->
                val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
                entityId in state.getZone(graveyardZone)
            }
            if (graveyardOwner != null) {
                ChosenTarget.Card(entityId, graveyardOwner, Zone.GRAVEYARD)
            } else {
                ChosenTarget.Permanent(entityId)
            }
        }
    }
}

/**
 * Merges the result of a sub-operation with accumulated events and continues.
 *
 * Handles the common paused/error/success branching pattern:
 * - Paused: returns immediately with merged events
 * - Error: returns immediately with merged events and error
 * - Success: recursively checks for more continuations (if checkForMore provided)
 *            or returns success with merged events
 */
fun mergeAndContinue(
    result: ExecutionResult,
    events: List<GameEvent>,
    checkForMore: CheckForMore? = null
): ExecutionResult {
    if (result.isPaused) {
        return ExecutionResult.paused(
            result.state,
            result.pendingDecision!!,
            events + result.events
        )
    }

    if (!result.isSuccess) {
        return ExecutionResult(
            state = result.state,
            events = events + result.events,
            error = result.error
        )
    }

    val mergedEvents = events + result.events
    return if (checkForMore != null) {
        checkForMore(result.newState, mergedEvents)
    } else {
        ExecutionResult.success(result.newState, mergedEvents)
    }
}
