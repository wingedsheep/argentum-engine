package com.wingedsheep.engine.mechanics.monarch

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.MonarchChangedEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * The single writer of [GameState.monarch] (CR 725, "The Monarch").
 *
 * Three callers move the title, and they all come through [become] so the CR 725.3 hand-off — the
 * old monarch stops being it as the new one starts — can't be done half-way:
 *
 * - [com.wingedsheep.engine.handlers.effects.player.BecomeMonarchExecutor], for "you become the
 *   monarch" and the inherent combat-damage steal (CR 725.2), which is that same effect;
 * - [onPlayerLeft], the CR 725.4 hand-off when the monarch leaves the game.
 *
 * The two inherent triggered abilities themselves live in
 * [com.wingedsheep.engine.event.MonarchAbilities].
 */
object MonarchService {

    /** Log/stack label for the sourceless monarch abilities and the leave-the-game hand-off. */
    const val SOURCE_NAME: String = "The Monarch"

    /**
     * Make [playerId] the monarch (CR 725.3). A player who already is the monarch stays it with no
     * event — nothing changed, so nothing should trigger or animate.
     */
    fun become(state: GameState, playerId: EntityId, sourceName: String): Pair<GameState, List<GameEvent>> {
        val previous = state.monarch
        if (previous == playerId) return state to emptyList()
        return state.copy(monarch = playerId) to
            listOf(MonarchChangedEvent(previousMonarchId = previous, newMonarchId = playerId, sourceName = sourceName))
    }

    /**
     * CR 725.4 — if the monarch leaves the game, the active player becomes the monarch at the same
     * time; if the active player is leaving too (or there is none), the next player in turn order
     * does; if nobody still in the game can, the game continues with no monarch.
     *
     * Called by the leave-the-game processor once [leaver] is already marked as lost, so
     * [GameState.activePlayers] no longer contains them (nor anyone else who lost alongside them).
     */
    fun onPlayerLeft(state: GameState, leaver: EntityId): Pair<GameState, List<GameEvent>> {
        if (state.monarch != leaver) return state to emptyList()
        val remaining = state.activePlayers
        val successor = successorFor(state, remaining)
        return state.copy(monarch = successor) to
            listOf(MonarchChangedEvent(previousMonarchId = leaver, newMonarchId = successor, sourceName = SOURCE_NAME))
    }

    /** The active player if still in the game, else the first still-in player after them in turn order. */
    private fun successorFor(state: GameState, remaining: List<EntityId>): EntityId? {
        if (remaining.isEmpty()) return null
        val active = state.activePlayerId ?: return remaining.first()
        if (active in remaining) return active
        val order = state.turnOrder
        val start = order.indexOf(active)
        if (start < 0) return remaining.first()
        return (1..order.size).asSequence()
            .map { order[(start + it) % order.size] }
            .firstOrNull { it in remaining }
    }
}
