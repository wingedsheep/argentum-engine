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
    /**
     * The object an enclosing `ForEach` loop is visiting — what `EffectTarget.IterationEntity`
     * names — captured with its identity when the loop bound it. Carried with the rest of the
     * environment, so it survives every pause inside the loop body and is inherited by a delayed
     * trigger the body creates.
     */
    val iteration: CapturedObjectBinding? = null,
) {
    fun followed(reference: ObjectRef): ObjectRef {
        var current = reference
        for (move in permittedMoves) if (move.oldObject == current) current = move.newObject
        return current
    }

    /** One departed melded object can become two cards, and each branch keeps its own identity. */
    fun followedObjects(reference: ObjectRef): List<ObjectRef> {
        var current = listOf(reference)
        for (move in permittedMoves) {
            if (move.oldObject in current) {
                current = current.flatMap {
                    if (it == move.oldObject) listOf(move.newObject) + move.additionalObjects else listOf(it)
                }
            }
        }
        return current.distinct()
    }

    /** Only this resolution's authorized meld split is followed; unrelated later visits are excluded. */
    fun meldedCards(entityId: com.wingedsheep.sdk.model.EntityId, state: GameState): List<com.wingedsheep.sdk.model.EntityId>? {
        val split = permittedMoves.firstOrNull { it.oldObject.entityId == entityId && it.additionalObjects.isNotEmpty() }
            ?: return null
        return followedObjects(split.oldObject).filter(state::isCurrentObject).map { it.entityId }
    }

    fun isCurrent(reference: ObjectRef?, state: GameState): Boolean =
        if (reference == null) !captured
        else if (permittedMoves.none { it.additionalObjects.isNotEmpty() }) state.isCurrentObject(followed(reference))
        else followedObjects(reference).any(state::isCurrentObject)

    /** Whether [iteration] still names the object the loop bound (a player always does). */
    fun isIterationCurrent(state: GameState): Boolean = iteration?.let { binding ->
        binding.entityId in state.turnOrder || binding.objectRef?.let { isCurrent(it, state) } == true
    } ?: false

    fun authorize(events: List<GameEvent>): ObjectReferenceEnvironment {
        val zoneEvents = events.filterIsInstance<ZoneChangeEvent>()
        val partnerArrivals = zoneEvents.filter { it.meldedPermanent != null }
            .groupBy { it.meldedPermanent to it.transitionCause }
        val moves = zoneEvents.mapNotNull { event ->
            if (event.transitionCause != com.wingedsheep.engine.core.ZoneTransitionCause.PRIMARY ||
                event.meldedPermanent != null) return@mapNotNull null
            val old = event.oldObject ?: return@mapNotNull null
            val new = event.newObject ?: return@mapNotNull null
            if (event.toZone !in PUBLIC_OBJECT_ZONES) return@mapNotNull null
            val partners = partnerArrivals[old to event.transitionCause].orEmpty().mapNotNull { it.newObject }
            PermittedObjectMove(old, new, partners)
        }
        return if (moves.isEmpty()) this else copy(permittedMoves = (permittedMoves + moves).distinct())
    }
}

@Serializable
data class PermittedObjectMove(
    val oldObject: ObjectRef,
    val newObject: ObjectRef,
    val additionalObjects: List<ObjectRef> = emptyList()
)

internal val PUBLIC_OBJECT_ZONES = setOf(Zone.BATTLEFIELD, Zone.GRAVEYARD, Zone.EXILE, Zone.STACK, Zone.COMMAND)

/** A present binding with a missing object reference is lost, never an instruction to recapture. */
@Serializable
data class CapturedObjectBinding(val entityId: com.wingedsheep.sdk.model.EntityId, val objectRef: ObjectRef?)

/** A zone-change trigger can find both cards that its triggering meld became in a public zone. */
internal fun meldArrivalMoves(events: List<GameEvent>, reference: ObjectRef?): List<PermittedObjectMove> {
    if (reference == null || events.none { it is ZoneChangeEvent && it.meldedPermanent != null }) return emptyList()
    val departure = events.firstOrNull {
        it is ZoneChangeEvent && it.newObject == reference && it.meldedPermanent == null && it.toZone in PUBLIC_OBJECT_ZONES
    } as? ZoneChangeEvent ?: return emptyList()
    val partners = events.filterIsInstance<ZoneChangeEvent>().filter {
        it.meldedPermanent != null && it.meldedPermanent == departure.oldObject
    }.mapNotNull { it.newObject }
    return if (partners.isEmpty()) emptyList() else listOf(PermittedObjectMove(reference, reference, partners))
}
