package com.wingedsheep.engine.handlers.actions.room

import com.wingedsheep.engine.core.DoorLockedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.sdk.model.EntityId

/**
 * Shared door-lock primitive (CR 709.5g) — the twin of [RoomDoorUnlocker]. Removes the "unlocked"
 * designation from a single unlocked face of a Room permanent and emits the resulting event.
 *
 * This is the one place that re-locks a [RoomComponent] door. Unlike unlocking, locking is not a
 * trigger source and can never fully unlock a Room, so it emits only a [DoorLockedEvent] — there is
 * no companion "fully unlocked" event and nothing for "When you unlock this door" triggers to fire
 * on. The locked half's name/cost/text turn off through normal projection of [RoomComponent.unlocked].
 */
object RoomDoorLocker {

    /**
     * Lock [faceId] of the Room [roomId] controlled by [controllerId]. Returns the new state with
     * that face's "unlocked" designation removed, plus a [DoorLockedEvent]. If the face is already
     * locked or the entity isn't a Room, returns the state unchanged with no events.
     *
     * Locking a half turns its static abilities *off* (CR 709.5), the mirror of [RoomDoorUnlocker];
     * so we re-bake the Room's cached
     * [com.wingedsheep.engine.state.components.battlefield.ContinuousEffectSourceComponent] via
     * [staticAbilityHandler] (with its replacement-effect component), the same refresh unlocking
     * does, to drop the now-locked half's continuous statics and replacement effects — including
     * when the locked half was the only one that had any.
     */
    fun lock(
        state: GameState,
        roomId: EntityId,
        faceId: RoomFaceId,
        controllerId: EntityId,
        staticAbilityHandler: StaticAbilityHandler,
    ): Pair<GameState, List<GameEvent>> {
        val container = state.getEntity(roomId) ?: return state to emptyList()
        val room = container.get<RoomComponent>() ?: return state to emptyList()
        val face = room.faces.find { it.id == faceId } ?: return state to emptyList()
        if (!room.isUnlocked(faceId)) return state to emptyList()

        val roomName = container.get<CardComponent>()?.name ?: face.name
        val updatedRoom = room.copy(unlocked = room.unlocked - faceId)
        val newState = state.updateEntity(roomId) { c ->
            staticAbilityHandler.rebakeRoomAbilityComponents(c.with(updatedRoom))
        }

        val event = DoorLockedEvent(
            roomId = roomId,
            roomName = roomName,
            faceId = faceId,
            faceName = face.name,
            controllerId = controllerId,
        )
        return newState to listOf(event)
    }
}
