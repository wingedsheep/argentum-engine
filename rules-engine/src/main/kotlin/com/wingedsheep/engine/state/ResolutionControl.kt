package com.wingedsheep.engine.state

import com.wingedsheep.engine.core.EndResolutionControlContinuation
import com.wingedsheep.engine.core.ResolutionControlEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** The stack visit is the lifetime, never the stable card id. List order is creation order. */
@Serializable
data class ResolutionControl(
    val resolvingObject: ObjectRef,
    val playerId: EntityId,
    val controllerId: EntityId,
)

fun GameState.isResolving(ref: ObjectRef): Boolean = continuationStack.any {
    it is EndResolutionControlContinuation && it.resolvingObject == ref
}

/** Rules control restricts out-of-game choices; session hotseat alone does not. */
fun GameState.isPlayerControlledByEffect(playerId: EntityId): Boolean {
    resolutionControls.lastOrNull { it.playerId == playerId && isResolving(it.resolvingObject) }
        ?.let { return it.controllerId != playerId }
    val hijack = getEntity(playerId)?.get<com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent>()
    return hijack?.state == com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent.HijackState.ACTIVE &&
        hijack.controllerId != playerId
}

/** Remember hand identities seen during a control window, like an explicit look-at-hand effect.
 * This is knowledge of those objects, not a permission to inspect cards acquired later.
 */
fun GameState.rememberResolutionControlHands(ref: ObjectRef): GameState {
    // An automatic end frame has already been popped by its dispatcher. Reconstruct only the
    // authority lookup, without returning the temporary continuation shape.
    val observing = if (isResolving(ref)) this else copy(
        continuationStack = continuationStack + EndResolutionControlContinuation(ref))
    var remembered = this
    for (player in resolutionControls.filter { it.resolvingObject == ref }.map { it.playerId }.distinct()) {
        val observer = observing.actorFor(player)
        if (observer != player) remembered =
            com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils.markRevealed(
                remembered, remembered.getHand(player), setOf(observer))
    }
    return remembered
}

/** Start only after the stack object's resolution-time validity checks succeed. */
fun GameState.beginResolutionControl(objectId: EntityId): ExecutionResult {
    val ref = objectRef(objectId) ?: return ExecutionResult.error(this, "Missing resolving object identity")
    val active = pushContinuation(EndResolutionControlContinuation(ref)).rememberResolutionControlHands(ref)
    return ExecutionResult.success(active, resolutionControls.filter { it.resolvingObject == ref }.map {
        ResolutionControlEvent(it, ResolutionControlEvent.Stage.STARTED)
    })
}

/** Called below every frame belonging to the completed resolution, including its final zone move. */
fun GameState.endResolutionControl(ref: ObjectRef, wasResolving: Boolean = isResolving(ref)): ExecutionResult {
    val ending = resolutionControls.filter { it.resolvingObject == ref }
    val finished = (if (wasResolving) rememberResolutionControlHands(ref) else this).copy(
        resolutionControls = resolutionControls.filterNot { it.resolvingObject == ref },
        continuationStack = continuationStack.filterNot {
            it is EndResolutionControlContinuation && it.resolvingObject == ref
        },
    )
    return ExecutionResult.success(finished, ending.map {
        ResolutionControlEvent(it, ResolutionControlEvent.Stage.ENDED)
    })
}
