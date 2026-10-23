package com.wingedsheep.engine.mechanics.dungeon

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DungeonCompletedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.OptionMetadata
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.VentureChooseDungeonContinuation
import com.wingedsheep.engine.core.VentureChooseRoomContinuation
import com.wingedsheep.engine.core.AnswerContinuation
import com.wingedsheep.engine.core.VenturedIntoDungeonEvent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.DungeonCardComponent
import com.wingedsheep.engine.state.components.player.DungeonMarker
import com.wingedsheep.engine.state.components.player.DungeonProgressComponent
import com.wingedsheep.sdk.dungeons.Dungeons
import com.wingedsheep.sdk.model.DungeonDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * The single writer of a player's dungeon state (CR 309, CR 701.49): the dungeon card in their
 * command zone, the venture marker on it, and the dungeons they've completed.
 *
 * Venturing is a short state machine — enter a dungeon (choosing which), or move the marker (choosing
 * which arrow where a room has several), completing the dungeon first if the marker is already on its
 * bottommost room. [venture] runs it up to the first choice and hands back either the finished state
 * or the question to ask; the continuation resumer calls [enter] / [moveTo] with the answer.
 *
 * Every move into a room emits [VenturedIntoDungeonEvent], from which the trigger detector creates
 * that room's ability (CR 309.4c). Completion emits [DungeonCompletedEvent] (CR 309.7).
 */
object DungeonService {

    /** Prefix of a dungeon option's [OptionMetadata.id] — `dungeon:<name>`. The client keys its dungeon picker off it. */
    const val DUNGEON_OPTION_PREFIX = "dungeon:"

    /** Prefix of a room option's [OptionMetadata.id] — `dungeon-room:<index>`. The client keys its dungeon map off it. */
    const val ROOM_OPTION_PREFIX = "dungeon-room:"

    fun progress(state: GameState, playerId: EntityId): DungeonProgressComponent? =
        state.getEntity(playerId)?.get<DungeonProgressComponent>()

    /** Where [playerId]'s venture marker is, or null if they own no dungeon in the command zone. */
    fun marker(state: GameState, playerId: EntityId): DungeonMarker? = progress(state, playerId)?.current

    /** How many dungeons [playerId] has completed — of [dungeon] only, when it's given; once per name when [distinctNames]. */
    fun completedCount(
        state: GameState,
        playerId: EntityId,
        dungeon: String? = null,
        distinctNames: Boolean = false
    ): Int {
        val completed = progress(state, playerId)?.completed ?: return 0
        val matching = if (dungeon == null) completed else completed.filter { it == dungeon }
        return if (distinctNames) matching.distinct().size else matching.size
    }

    /** The outcome of running a venture up to its first choice. */
    sealed interface Step {
        val state: GameState
        val events: List<GameEvent>

        /** The venture finished — the marker is in its new room. */
        data class Done(override val state: GameState, override val events: List<GameEvent>) : Step

        /** The venturing player has to choose; ask [decision] and resume with [continuation]. */
        data class Choose(
            override val state: GameState,
            override val events: List<GameEvent>,
            val decision: (String) -> PendingDecision,
            val continuation: AnswerContinuation
        ) : Step
    }

    /**
     * "[playerId] ventures into the dungeon" (CR 701.49), or into the dungeon named [into] (CR
     * 701.49d). Runs until the marker has moved or a choice is needed.
     */
    fun venture(
        state: GameState,
        playerId: EntityId,
        into: String?,
        sourceName: String,
        sourceId: EntityId?
    ): Step {
        val marker = marker(state, playerId)
        val dungeon = marker?.let { Dungeons.byName(it.dungeon) }
        if (marker != null && dungeon != null && !dungeon.isBottommost(marker.room)) {
            // CR 701.49b — advance along an arrow; the player picks one where there are several.
            val next = dungeon.rooms[marker.room].leadsTo
            if (next.size == 1) {
                val (moved, events) = moveTo(state, playerId, next.single(), sourceName)
                return Step.Done(moved, events)
            }
            return Step.Choose(
                state,
                emptyList(),
                decision = { id -> chooseRoomDecision(id, playerId, dungeon, next, sourceName, sourceId) },
                continuation = VentureChooseRoomContinuation(
                    playerId = playerId,
                    dungeonEntityId = marker.dungeonEntityId,
                    dungeonName = dungeon.name,
                    candidates = next,
                    sourceName = sourceName
                )
            )
        }

        // CR 701.49c — on the bottommost room, venturing completes the dungeon first, then starts a
        // new one. (Usually the state-based action has already removed it; this is the case of a
        // venture while the bottom room's ability is still on the stack.)
        var current = state
        val events = mutableListOf<GameEvent>()
        if (marker != null) {
            val (completed, completionEvents) = complete(current, playerId)
            current = completed
            events += completionEvents
        }

        // CR 701.49a / 309.2a — choose a dungeon from outside the game and enter its topmost room.
        val candidates = Dungeons.choosable(into)
        if (candidates.isEmpty()) return Step.Done(current, events)
        if (candidates.size == 1) {
            val (entered, enterEvents) = enter(current, playerId, candidates.single(), sourceName)
            return Step.Done(entered, events + enterEvents)
        }
        return Step.Choose(
            current,
            events,
            decision = { id -> chooseDungeonDecision(id, playerId, candidates, sourceName, sourceId) },
            continuation = VentureChooseDungeonContinuation(
                playerId = playerId,
                candidates = candidates.map { it.name },
                sourceName = sourceName
            )
        )
    }

    /**
     * Put [dungeon] into [playerId]'s command zone with the venture marker on its topmost room
     * (CR 309.4a). The player must not already own a dungeon there (CR 309.3).
     */
    fun enter(
        state: GameState,
        playerId: EntityId,
        dungeon: DungeonDefinition,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        check(marker(state, playerId) == null) { "A player can own only one dungeon in the command zone (CR 309.3)" }
        val (cardId, minted) = state.newEntity()
        val withCard = minted.withEntity(cardId, ComponentContainer.of(DungeonCardComponent(dungeon.name, playerId)))
        val room = dungeon.topmostRoom
        val updated = setMarker(withCard, playerId, DungeonMarker(cardId, dungeon.name, room))
        return updated to listOf(
            VenturedIntoDungeonEvent(
                playerId = playerId,
                dungeonEntityId = cardId,
                dungeonName = dungeon.name,
                roomIndex = room,
                roomName = dungeon.rooms[room].name,
                enteredDungeon = true,
                sourceName = sourceName
            )
        )
    }

    /** Move [playerId]'s venture marker into room [room] of their current dungeon (CR 309.5a). */
    fun moveTo(
        state: GameState,
        playerId: EntityId,
        room: Int,
        sourceName: String
    ): Pair<GameState, List<GameEvent>> {
        val marker = marker(state, playerId) ?: return state to emptyList()
        val dungeon = Dungeons.byName(marker.dungeon) ?: return state to emptyList()
        val updated = setMarker(state, playerId, marker.copy(room = room))
        return updated to listOf(
            VenturedIntoDungeonEvent(
                playerId = playerId,
                dungeonEntityId = marker.dungeonEntityId,
                dungeonName = dungeon.name,
                roomIndex = room,
                roomName = dungeon.rooms[room].name,
                enteredDungeon = false,
                sourceName = sourceName
            )
        )
    }

    /**
     * Remove [playerId]'s dungeon from the game — they complete it (CR 309.7). Called by the
     * state-based action (CR 704.5t) and by a venture from the bottommost room (CR 701.49c).
     */
    fun complete(state: GameState, playerId: EntityId): Pair<GameState, List<GameEvent>> {
        val progress = progress(state, playerId) ?: return state to emptyList()
        val marker = progress.current ?: return state to emptyList()
        val completed = progress.completed + marker.dungeon
        val updated = state
            .withoutEntity(marker.dungeonEntityId)
            .updateEntity(playerId) { it.with(DungeonProgressComponent(current = null, completed = completed)) }
        return updated to listOf(DungeonCompletedEvent(playerId, marker.dungeon, completed.size))
    }

    /**
     * A player who leaves the game takes their dungeon card with them (CR 800.4a). That is not
     * completing it — no event, no count.
     */
    fun onPlayerLeft(state: GameState, leaver: EntityId): GameState {
        val progress = progress(state, leaver) ?: return state
        val marker = progress.current ?: return state
        return state.withoutEntity(marker.dungeonEntityId)
            .updateEntity(leaver) { it.with(progress.copy(current = null)) }
    }

    private fun setMarker(state: GameState, playerId: EntityId, marker: DungeonMarker): GameState {
        val previous = progress(state, playerId) ?: DungeonProgressComponent()
        return state.updateEntity(playerId) { it.with(previous.copy(current = marker)) }
    }

    private fun chooseDungeonDecision(
        id: String,
        playerId: EntityId,
        candidates: List<DungeonDefinition>,
        sourceName: String,
        sourceId: EntityId?
    ): PendingDecision = ChooseOptionDecision(
        id = id,
        playerId = playerId,
        prompt = "Venture into the dungeon — choose a dungeon",
        context = DecisionContext(sourceId = sourceId, sourceName = sourceName, phase = DecisionPhase.RESOLUTION),
        options = candidates.map { it.name },
        optionMetadata = candidates.map { dungeon ->
            OptionMetadata(
                id = DUNGEON_OPTION_PREFIX + dungeon.name,
                description = "${dungeon.rooms.size} rooms",
                imageUri = dungeon.imageUri
            )
        }
    )

    private fun chooseRoomDecision(
        id: String,
        playerId: EntityId,
        dungeon: DungeonDefinition,
        next: List<Int>,
        sourceName: String,
        sourceId: EntityId?
    ): PendingDecision = ChooseOptionDecision(
        id = id,
        playerId = playerId,
        prompt = "Venture into the dungeon — choose the next room of ${dungeon.name}",
        context = DecisionContext(sourceId = sourceId, sourceName = sourceName, phase = DecisionPhase.RESOLUTION),
        options = next.map { dungeon.rooms[it].name },
        optionMetadata = next.map { room ->
            OptionMetadata(id = ROOM_OPTION_PREFIX + room, description = dungeon.rooms[room].text)
        }
    )
}
