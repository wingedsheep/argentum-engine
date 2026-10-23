package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.EffectProgram
import com.wingedsheep.engine.core.GatedActionContinuation
import com.wingedsheep.engine.core.ForEachContinuation
import com.wingedsheep.engine.core.ReflexiveTriggerTargetContinuation
import com.wingedsheep.engine.core.RepeatWhileContinuation
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineOutputs
import com.wingedsheep.engine.handlers.mergeOutputs
import com.wingedsheep.engine.handlers.pipelineOutputs
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
 * - [EffectProgram] — remaining composite siblings read the collections
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
 * [RepeatWhileContinuation] takes [collections] and [numbers], into its `bodyCollections` /
 * `bodyNumbers` slots — a repeat condition reads a pass's collections (Cultivator Colossus) or its
 * numbers (Delina, Wild Mage's d20 result gating "you may roll again").
 *
 * The consumer is always the top of the continuation stack here: triggers waiting to go on the
 * stack live in the state ([GameState.triggersBeingPlaced], [GameState.pendingTriggers]), never
 * on the continuation stack, so nothing sits between a producer and its consumer.
 */
fun exposeCollectionsToNextFrame(
    state: GameState,
    collections: Map<String, List<EntityId>>,
    numbers: Map<String, Int> = emptyMap(),
    chosenValues: Map<String, String> = emptyMap(),
    subtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
    sacrificed: List<com.wingedsheep.engine.state.components.stack.EntitySnapshot> = emptyList(),
    departed: List<com.wingedsheep.engine.state.components.stack.EntitySnapshot> = emptyList(),
): GameState = exposeOutputsToNextFrame(state, PipelineOutputs(
    collections = collections, subtypeGroups = subtypeGroups, numbers = numbers,
    chosenValues = chosenValues, sacrificed = sacrificed, departed = departed,
))

/**
 * A step a resumer ran to completion hands the frame beneath every output it stored: what the
 * program running it would have published had something inside it paused, so the steps after it
 * see the same values on both paths.
 */
fun com.wingedsheep.engine.core.EffectResult.stateWithOutputsHandedOn(): GameState =
    exposeOutputsToNextFrame(state, pipelineOutputs)

/** [exposeCollectionsToNextFrame] for a whole [PipelineOutputs]; merges with [mergeOutputs]. */
fun exposeOutputsToNextFrame(state: GameState, outputs: PipelineOutputs): GameState {
    if (outputs.isEmpty()) return state

    // The same merge a synchronous step's result gets, with snapshots merged as a set.
    fun EffectContext.withMergedCollections(): EffectContext = mergeOutputs(outputs, dedupSnapshots = true)

    return when (val next = state.peekContinuation()) {
        is com.wingedsheep.engine.core.ManaAbilitySourcesContinuation,
        is com.wingedsheep.engine.core.ManaSpendingObligationsContinuation -> {
            val (_, popped) = state.popContinuation()
            exposeOutputsToNextFrame(popped, outputs).pushContinuation(next)
        }
        is ForEachContinuation -> {
            val merged = next.effectContext.withMergedCollections()
            var accumulated = next.effectContext.pipeline.storedCollections
            for ((localName, aggregateName) in next.effect.collectCollections) {
                val iterationOutput = outputs.collections[localName].orEmpty()
                if (iterationOutput.isNotEmpty()) {
                    accumulated = accumulated + (
                        aggregateName to accumulated[aggregateName].orEmpty() + iterationOutput
                    )
                }
            }
            val (_, popped) = state.popContinuation()
            popped.pushContinuation(
                next.copy(
                    effectContext = merged.copy(
                        pipeline = merged.pipeline.copy(storedCollections = accumulated)
                    )
                )
            )
        }
        is EffectProgram -> {
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
            popped.pushContinuation(
                next.copy(
                    bodyCollections = next.bodyCollections + outputs.collections,
                    bodyNumbers = next.bodyNumbers + outputs.numbers,
                )
            )
        }
        else -> state
    }
}
