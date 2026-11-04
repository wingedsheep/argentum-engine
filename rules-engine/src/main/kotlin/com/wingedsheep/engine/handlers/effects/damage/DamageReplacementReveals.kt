package com.wingedsheep.engine.handlers.effects.damage

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.TurnFaceUpEvent
import com.wingedsheep.engine.mechanics.FaceUpInstead
import com.wingedsheep.engine.state.GameState

/**
 * A chosen face-up replacement reveals the permanent before the next choice. Other damage
 * mutations remain transactional. Keep only the original revealed entities, so replay can read
 * the original characteristics without storing a second game state or repeating reveal events.
 */
object DamageReplacementReveals {
    fun begin(state: GameState): GameState {
        if (state.damageReplacementRevealOriginals.isEmpty()) return state
        return state.copy(
            entities = state.entities + state.damageReplacementRevealOriginals,
            timestamp = state.damageReplacementRevealTimestamp ?: state.timestamp
        )
    }

    fun pause(
        state: GameState,
        working: GameState,
        events: List<GameEvent>
    ): Pair<GameState, List<GameEvent>> {
        val reveals = events.filterIsInstance<TurnFaceUpEvent>().distinctBy { it.entityId }
        if (reveals.isEmpty()) return state to emptyList()
        val originals = state.damageReplacementRevealOriginals.toMutableMap()
        val alreadyShown = originals.keys.toSet()
        val base = begin(state)
        var visible = base
        val newEvents = mutableListOf<GameEvent>()
        for (reveal in reveals) {
            val id = reveal.entityId
            val original = base.getEntity(id) ?: continue
            // Only an actual turn-up performed by this replacement transaction is publishable.
            if (FaceUpInstead.applies(working, id)) continue
            originals.putIfAbsent(id, original)
            val (next, event) = FaceUpInstead.turnFaceUp(visible, id)
            visible = next
            if (id !in alreadyShown && event != null) newEvents += event
        }
        return visible.copy(
            damageReplacementRevealOriginals = originals,
            damageReplacementRevealTimestamp = state.damageReplacementRevealTimestamp ?: base.timestamp
        ) to newEvents
    }

    fun finish(state: GameState, events: List<GameEvent>): Pair<GameState, List<GameEvent>> {
        val alreadyShown = state.damageReplacementRevealOriginals.keys
        val newEvents = events.filterNot { it is TurnFaceUpEvent && it.entityId in alreadyShown }
        return state.copy(
            damageReplacementRevealOriginals = emptyMap(),
            damageReplacementRevealTimestamp = null
        ) to newEvents
    }
}
