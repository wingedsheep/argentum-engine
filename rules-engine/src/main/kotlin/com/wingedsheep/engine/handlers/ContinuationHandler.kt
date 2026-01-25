package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles resumption of execution after a player decision.
 *
 * When the engine pauses for player input, it pushes a ContinuationFrame
 * onto the state's continuation stack. When the player submits their decision,
 * this handler pops the frame and resumes execution based on the frame type.
 */
class ContinuationHandler(
    private val effectExecutorRegistry: EffectExecutorRegistry
) {

    /**
     * Resume execution after a decision is submitted.
     *
     * @param state The game state with the pending decision already cleared
     * @param response The player's decision response
     * @return The result of resuming execution
     */
    fun resume(state: GameState, response: DecisionResponse): ExecutionResult {
        val (continuation, stateAfterPop) = state.popContinuation()

        if (continuation == null) {
            // No continuation frame - this shouldn't happen if used correctly
            return ExecutionResult.success(state)
        }

        // Verify the decision ID matches
        if (continuation.decisionId != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${continuation.decisionId}, got ${response.decisionId}"
            )
        }

        return when (continuation) {
            is DiscardContinuation -> resumeDiscard(stateAfterPop, continuation, response)
            is ScryContinuation -> resumeScry(stateAfterPop, continuation, response)
            is EffectContinuation -> resumeEffect(stateAfterPop, continuation, response)
            is TriggeredAbilityContinuation -> resumeTriggeredAbility(stateAfterPop, continuation, response)
            is DamageAssignmentContinuation -> resumeDamageAssignment(stateAfterPop, continuation, response)
            is ResolveSpellContinuation -> resumeSpellResolution(stateAfterPop, continuation, response)
            is SacrificeContinuation -> resumeSacrifice(stateAfterPop, continuation, response)
            is MayAbilityContinuation -> resumeMayAbility(stateAfterPop, continuation, response)
            is HandSizeDiscardContinuation -> resumeHandSizeDiscard(stateAfterPop, continuation, response)
            else -> ExecutionResult.success(stateAfterPop) // Other continuations handled by other agents
        }
    }

    /**
     * Resume after player selected cards to discard.
     */
    private fun resumeDiscard(
        state: GameState,
        continuation: DiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Check if there are more continuations to process
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player ordered cards for scry.
     */
    private fun resumeScry(
        state: GameState,
        continuation: ScryContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is PilesSplitResponse) {
            return ExecutionResult.error(state, "Expected pile split response for scry")
        }

        val playerId = continuation.playerId

        // Pile 0 = top of library, Pile 1 = bottom of library
        val topCards = response.piles.getOrElse(0) { emptyList() }
        val bottomCards = response.piles.getOrElse(1) { emptyList() }

        var newState = state
        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)

        // Remove all scried cards from library first
        val allScried = topCards + bottomCards
        for (cardId in allScried) {
            newState = newState.removeFromZone(libraryZone, cardId)
        }

        // Get current library
        val currentLibrary = newState.getZone(libraryZone).toMutableList()

        // Add top cards to top of library (in order)
        val newLibrary = topCards + currentLibrary + bottomCards

        // Update zone with new order
        newState = newState.copy(
            zones = newState.zones + (libraryZone to newLibrary)
        )

        val events = listOf(
            ScryCompletedEvent(playerId, topCards.size, bottomCards.size)
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume a composite effect with remaining effects.
     */
    private fun resumeEffect(
        state: GameState,
        continuation: EffectContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        // First, process any inner continuation that was awaiting this decision
        // The response was already handled by a nested handler

        // Now continue with remaining effects
        val context = continuation.toEffectContext()
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for (effect in continuation.remainingEffects) {
            val result = effectExecutorRegistry.execute(currentState, effect, context)

            if (!result.isSuccess) {
                return result
            }

            if (result.isPaused) {
                // Another effect needs a decision - update the continuation with remaining effects
                val remainingIndex = continuation.remainingEffects.indexOf(effect)
                val stillRemaining = continuation.remainingEffects.drop(remainingIndex + 1)

                if (stillRemaining.isNotEmpty()) {
                    val newContinuation = EffectContinuation(
                        decisionId = result.pendingDecision!!.id,
                        remainingEffects = stillRemaining,
                        sourceId = continuation.sourceId,
                        controllerId = continuation.controllerId,
                        opponentId = continuation.opponentId,
                        xValue = continuation.xValue
                    )
                    val stateWithCont = result.state.pushContinuation(newContinuation)
                    return ExecutionResult.paused(
                        stateWithCont,
                        result.pendingDecision,
                        allEvents + result.events
                    )
                }

                // No more effects after this one
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = result.state
            allEvents.addAll(result.events)
        }

        return checkForMoreContinuations(currentState, allEvents)
    }

    /**
     * Resume a triggered ability after target selection.
     */
    private fun resumeTriggeredAbility(
        state: GameState,
        continuation: TriggeredAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        // TODO: Implement triggered ability resumption
        // This would need to store the selected targets and continue resolution
        return ExecutionResult.success(state)
    }

    /**
     * Resume combat damage assignment.
     */
    private fun resumeDamageAssignment(
        state: GameState,
        continuation: DamageAssignmentContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is DamageAssignmentResponse) {
            return ExecutionResult.error(state, "Expected damage assignment response")
        }

        // TODO: Apply the damage assignments from the response
        // This would need to work with the CombatManager

        return ExecutionResult.success(state)
    }

    /**
     * Resume spell resolution after target/mode selection.
     */
    private fun resumeSpellResolution(
        state: GameState,
        continuation: ResolveSpellContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        // TODO: Implement spell resolution resumption
        // This would store targets/modes and continue resolution
        return ExecutionResult.success(state)
    }

    /**
     * Resume after player selected cards to sacrifice.
     */
    private fun resumeSacrifice(
        state: GameState,
        continuation: SacrificeContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for sacrifice")
        }

        val playerId = continuation.playerId
        val selectedPermanents = response.selectedCards

        // Move selected permanents from battlefield to graveyard
        var newState = state
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        for (permanentId in selectedPermanents) {
            newState = newState.removeFromZone(battlefieldZone, permanentId)
            newState = newState.addToZone(graveyardZone, permanentId)
        }

        val events = listOf(
            PermanentsSacrificedEvent(playerId, selectedPermanents)
        )

        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player selected cards to discard for hand size during cleanup.
     */
    private fun resumeHandSizeDiscard(
        state: GameState,
        continuation: HandSizeDiscardContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card selection response for hand size discard")
        }

        val playerId = continuation.playerId
        val selectedCards = response.selectedCards

        // Move selected cards from hand to graveyard
        var newState = state
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        for (cardId in selectedCards) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        val events = listOf(
            CardsDiscardedEvent(playerId, selectedCards)
        )

        // Check if there are more continuations to process
        return checkForMoreContinuations(newState, events)
    }

    /**
     * Resume after player made a yes/no choice.
     */
    private fun resumeMayAbility(
        state: GameState,
        continuation: MayAbilityContinuation,
        response: DecisionResponse
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for may ability")
        }

        val context = continuation.toEffectContext()
        val effectToExecute = if (response.choice) {
            continuation.effectIfYes
        } else {
            continuation.effectIfNo
        }

        if (effectToExecute == null) {
            // No effect to execute (player said no and there's no alternative)
            return checkForMoreContinuations(state, emptyList())
        }

        val result = effectExecutorRegistry.execute(state, effectToExecute, context)

        if (result.isPaused) {
            // The effect needs another decision
            return result
        }

        return checkForMoreContinuations(result.state, result.events.toList())
    }

    /**
     * Check if there are more continuation frames to process.
     * If there's an EffectContinuation waiting, process it.
     */
    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>
    ): ExecutionResult {
        val nextContinuation = state.peekContinuation()

        if (nextContinuation is EffectContinuation && nextContinuation.remainingEffects.isNotEmpty()) {
            // Pop and process the effect continuation
            val (_, stateAfterPop) = state.popContinuation()
            val context = nextContinuation.toEffectContext()
            var currentState = stateAfterPop
            val allEvents = events.toMutableList()

            for (effect in nextContinuation.remainingEffects) {
                val result = effectExecutorRegistry.execute(currentState, effect, context)

                if (!result.isSuccess) {
                    return ExecutionResult(result.state, allEvents + result.events, result.error)
                }

                if (result.isPaused) {
                    // Push remaining effects as new continuation
                    val remainingIndex = nextContinuation.remainingEffects.indexOf(effect)
                    val stillRemaining = nextContinuation.remainingEffects.drop(remainingIndex + 1)

                    var finalState = result.state
                    if (stillRemaining.isNotEmpty()) {
                        val newContinuation = EffectContinuation(
                            decisionId = result.pendingDecision!!.id,
                            remainingEffects = stillRemaining,
                            sourceId = nextContinuation.sourceId,
                            controllerId = nextContinuation.controllerId,
                            opponentId = nextContinuation.opponentId,
                            xValue = nextContinuation.xValue
                        )
                        finalState = finalState.pushContinuation(newContinuation)
                    }

                    return ExecutionResult.paused(
                        finalState,
                        result.pendingDecision!!,
                        allEvents + result.events
                    )
                }

                currentState = result.state
                allEvents.addAll(result.events)
            }

            return ExecutionResult.success(currentState, allEvents)
        }

        return ExecutionResult.success(state, events)
    }
}
