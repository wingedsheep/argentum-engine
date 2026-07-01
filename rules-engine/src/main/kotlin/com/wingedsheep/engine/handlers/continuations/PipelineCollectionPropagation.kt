package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.GatedActionContinuation
import com.wingedsheep.engine.core.ReflexiveTriggerTargetContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Merge pipeline [collections] produced by a resolved continuation into the next frame's
 * effect context, so the consumer beneath sees what the drained step stored.
 *
 * Pipeline storage lives in [EffectContext], which is frozen into each continuation frame
 * when it is pushed — a frame pushed *before* its action ran (the pre-push pattern) holds a
 * pre-action context. This helper is the propagation seam: every resumer that finishes a
 * collection-producing step routes its `updatedCollections` here, and the recognized
 * consumer frames get a merged context:
 *
 * - [EffectContinuation] — remaining composite siblings read the collections
 *   (the original LibraryAndZone inject pattern).
 * - [ReflexiveTriggerTargetContinuation] — "When you do, …" reflexive effects
 *   (the Amass multi-Army path).
 * - [GatedActionContinuation] — a `Gate.DoAction` frame whose
 *   [com.wingedsheep.sdk.scripting.effects.SuccessCriterion.CollectionNonEmpty] criterion
 *   (and `then`/`otherwise` branches) must see the collections the action produced.
 *
 * Unknown frame types are left untouched (they don't read pipeline storage).
 *
 * The consumer is always the top of the continuation stack here: a deferred
 * [PendingTriggersContinuation] queued by a mid-resolution trigger is inserted *beneath* the
 * frames of the in-flight resolution (see `SubmitDecisionHandler`), so it never sits between a
 * producer and its consumer.
 */
fun exposeCollectionsToNextFrame(
    state: GameState,
    collections: Map<String, List<EntityId>>
): GameState {
    if (collections.isEmpty()) return state

    fun EffectContext.withMergedCollections(): EffectContext =
        copy(pipeline = pipeline.copy(storedCollections = pipeline.storedCollections + collections))

    return when (val next = state.peekContinuation()) {
        is EffectContinuation -> {
            val (_, popped) = state.popContinuation()
            popped.pushContinuation(next.copy(effectContext = next.effectContext.withMergedCollections()))
        }
        is ReflexiveTriggerTargetContinuation -> {
            val (_, popped) = state.popContinuation()
            popped.pushContinuation(next.copy(effectContext = next.effectContext.withMergedCollections()))
        }
        is GatedActionContinuation -> {
            val (_, popped) = state.popContinuation()
            popped.pushContinuation(next.copy(effectContext = next.effectContext.withMergedCollections()))
        }
        else -> state
    }
}
