package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.Serializable

/** Identity permissions belonging to one spell or ability's resolution, including its costs. */
@Serializable
data class ObjectReferenceEnvironment(
    /** True even when the captured object no longer exists; null must never mean recapture. */
    val captured: Boolean = false,
    /** Original source object: never advanced when Self is allowed to follow a zone change. */
    val origin: ObjectRef? = null,
    val source: ObjectRef? = null,
    val triggering: ObjectRef? = null,
    /** Stack object identity distinguishes separate resolutions of the same source/ability. */
    val resolutionKey: String? = null,
    val permittedMoves: List<PermittedObjectMove> = emptyList(),
    /** A deliberately bound Self, independent of the ability’s originating source. */
    val selfBinding: CapturedObjectBinding? = null,
) {
    fun followed(reference: ObjectRef): ObjectRef {
        var current = reference
        // Generations increase on every move, so this chain cannot cycle.
        for (move in permittedMoves) if (move.oldObject == current) current = move.newObject
        return current
    }

    fun isCurrent(reference: ObjectRef?, state: GameState): Boolean =
        if (reference == null) !captured else state.isCurrentObject(followed(reference))

    fun isSelfCurrent(state: GameState): Boolean = selfBinding?.let { binding ->
        binding.entityId in state.turnOrder || binding.objectRef?.let { state.isCurrentObject(followed(it)) } == true
    } ?: isCurrent(source, state)

    fun authorize(events: List<GameEvent>): ObjectReferenceEnvironment {
        val moves = events.filterIsInstance<ZoneChangeEvent>().mapNotNull { event ->
            if (event.transitionCause != com.wingedsheep.engine.core.ZoneTransitionCause.PRIMARY) return@mapNotNull null
            val old = event.oldObject ?: return@mapNotNull null
            val new = event.newObject ?: return@mapNotNull null
            if (event.toZone !in PUBLIC_OBJECT_ZONES) return@mapNotNull null
            PermittedObjectMove(old, new)
        }
        return if (moves.isEmpty()) this else copy(permittedMoves = (permittedMoves + moves).distinct())
    }
}

@Serializable
data class PermittedObjectMove(val oldObject: ObjectRef, val newObject: ObjectRef)

internal val PUBLIC_OBJECT_ZONES = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.EXILE, Zone.STACK, Zone.COMMAND)

/** A present binding with a missing object reference is lost, never an instruction to recapture. */
@Serializable
data class CapturedObjectBinding(val entityId: com.wingedsheep.sdk.model.EntityId, val objectRef: ObjectRef?)
