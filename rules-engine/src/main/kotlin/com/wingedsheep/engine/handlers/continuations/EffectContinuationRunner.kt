package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Executes a list of effects in sequence, handling pauses, errors, and context updates.
 *
 * Used by both the EffectContinuation resumer (decision-resume path) and the EffectContinuation
 * auto-resumer (checkForMoreContinuations path) to avoid duplicating the loop.
 */
class EffectContinuationRunner(
    private val effectExecutorRegistry: EffectExecutorRegistry
) {

    fun executeRemainingEffects(
        initialState: GameState,
        effects: List<Effect>,
        initialContext: EffectContext
    ): EffectResult {
        var currentContext = initialContext
        var currentState = initialState
        val allEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for ((index, effect) in effects.withIndex()) {
            val stillRemaining = effects.drop(index + 1)

            val stateForExecution = if (stillRemaining.isNotEmpty()) {
                val remainingContinuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = stillRemaining,
                    effectContext = currentContext
                )
                currentState.pushContinuation(remainingContinuation)
            } else {
                currentState
            }

            val result = effectExecutorRegistry.execute(stateForExecution, effect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
                currentState = if (stillRemaining.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
                continue
            }

            if (result.isPaused) {
                return EffectResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (stillRemaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            if (result.updatedCollections.isNotEmpty() ||
                result.updatedSubtypeGroups.isNotEmpty() ||
                result.updatedStoredNumbers.isNotEmpty() ||
                result.updatedChosenValues.isNotEmpty()
            ) {
                currentContext = currentContext.copy(
                    pipeline = currentContext.pipeline.copy(
                        storedCollections = currentContext.pipeline.storedCollections + result.updatedCollections,
                        storedSubtypeGroups = currentContext.pipeline.storedSubtypeGroups + result.updatedSubtypeGroups,
                        storedNumbers = currentContext.pipeline.storedNumbers + result.updatedStoredNumbers,
                        chosenValues = currentContext.pipeline.chosenValues + result.updatedChosenValues
                    )
                )
            }

            // Thread sacrifice snapshots between resumed sibling effects so a later step can read
            // the sacrificed permanent's last-known P/T via DynamicAmount.Sacrificed — e.g.
            // The Gitrog, Ravenous Ride sacrifices a saddler (after a selection pause) and then
            // draws/puts lands equal to its power. Mirrors CompositeEffectExecutor's threading.
            if (result.updatedSacrificedPermanents.isNotEmpty()) {
                currentContext = currentContext.copy(
                    sacrificedPermanents = currentContext.sacrificedPermanents + result.updatedSacrificedPermanents
                )
            }
        }

        // Return accumulated collections / subtype groups / numbers / chosen values / sacrifices so callers can propagate them
        val accumulatedCollections = currentContext.pipeline.storedCollections - initialContext.pipeline.storedCollections.keys
        val accumulatedSubtypeGroups = currentContext.pipeline.storedSubtypeGroups - initialContext.pipeline.storedSubtypeGroups.keys
        val accumulatedStoredNumbers = currentContext.pipeline.storedNumbers - initialContext.pipeline.storedNumbers.keys
        val accumulatedChosenValues = currentContext.pipeline.chosenValues - initialContext.pipeline.chosenValues.keys
        val accumulatedSacrificed = currentContext.sacrificedPermanents.drop(initialContext.sacrificedPermanents.size)
        return EffectResult(
            currentState,
            allEvents,
            updatedCollections = accumulatedCollections,
            updatedSubtypeGroups = accumulatedSubtypeGroups,
            updatedStoredNumbers = accumulatedStoredNumbers,
            updatedChosenValues = accumulatedChosenValues,
            updatedSacrificedPermanents = accumulatedSacrificed
        )
    }
}
