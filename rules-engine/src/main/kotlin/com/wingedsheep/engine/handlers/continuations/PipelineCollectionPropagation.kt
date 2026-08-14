package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.GatedActionContinuation
import com.wingedsheep.engine.core.ReflexiveTriggerTargetContinuation
import com.wingedsheep.engine.core.RepeatWhileContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Merge pipeline [collections], [numbers], and [chosenValues] produced by a resolved continuation
 * into the next frame's effect context, so the consumer beneath sees what the drained step stored.
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
 * - [RepeatWhileContinuation] — an AFTER_BODY frame whose repeat condition (a WhileCondition,
 *   e.g. `CollectionContainsMatch("putting", …)` on Cultivator Colossus) evaluates against the
 *   body's own outputs *this pass*. When the body paused for a decision, its collections drain
 *   here; they're stashed in [RepeatWhileContinuation.bodyCollections] (NOT the frame's pristine
 *   `effectContext`) so the AFTER_BODY resumer can feed them to the condition as bodyOutputs while
 *   the next iteration still re-gathers fresh. Mirrors the synchronous path in RepeatWhileExecutor.
 *
 * Unknown frame types are left untouched (they don't read pipeline storage).
 *
 * [numbers] and [chosenValues] ride the same seam as [collections], because a pipeline step stores
 * all three the same way and a consumer beneath reads them the same way. Dropping them here is not
 * a harmless omission: `CompositeEffectExecutor` threads `updatedStoredNumbers` between siblings on
 * the synchronous path, so a composite that stores a number and then pauses would lose it while an
 * identical composite that never paused would keep it — the same card working or not depending on
 * whether a decision happened to be presented. Bolg of the North is the case in hand: it snapshots
 * the sacrificed creature's power into the pipeline before sacrificing it (the reflexive ability
 * resolves from a fresh context on the far side of a stack round-trip and cannot read the
 * sacrifice's LKI), and the sacrifice pauses for target selection as soon as you control two
 * creatures that could be sacrificed.
 *
 * [RepeatWhileContinuation] takes only [collections], into its `bodyCollections` slot — the repeat
 * condition reads collection outputs, and there is no numeric equivalent for it to read.
 *
 * The consumer is always the top of the continuation stack here: a deferred
 * [PendingTriggersContinuation] queued by a mid-resolution trigger is inserted *beneath* the
 * frames of the in-flight resolution (see `SubmitDecisionHandler`), so it never sits between a
 * producer and its consumer.
 */
fun exposeCollectionsToNextFrame(
    state: GameState,
    collections: Map<String, List<EntityId>>,
    numbers: Map<String, Int> = emptyMap(),
    chosenValues: Map<String, String> = emptyMap(),
): GameState {
    if (collections.isEmpty() && numbers.isEmpty() && chosenValues.isEmpty()) return state

    fun EffectContext.withMergedCollections(): EffectContext =
        copy(
            pipeline = pipeline.copy(
                storedCollections = pipeline.storedCollections + collections,
                storedNumbers = pipeline.storedNumbers + numbers,
                chosenValues = pipeline.chosenValues + chosenValues,
            )
        )

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
        is RepeatWhileContinuation -> {
            // Stash into bodyCollections, not effectContext — the condition reads these outputs
            // this pass, but the next iteration must start from the pristine pre-loop context.
            val (_, popped) = state.popContinuation()
            popped.pushContinuation(next.copy(bodyCollections = next.bodyCollections + collections))
        }
        else -> state
    }
}
