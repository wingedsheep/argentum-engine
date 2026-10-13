package com.wingedsheep.engine.handlers.actions.decision

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ContinuationHandler
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.identity.CardComponent
import kotlin.reflect.KClass

/**
 * Handler for the SubmitDecision action.
 *
 * Processes player responses to pending decisions, using the
 * continuation system to resume effect execution.
 */
class SubmitDecisionHandler(
    private val continuationHandler: ContinuationHandler
) : ActionHandler<SubmitDecision> {
    override val actionType: KClass<SubmitDecision> = SubmitDecision::class

    override fun validate(state: GameState, action: SubmitDecision): String? {
        val pending = state.pendingDecision
            ?: return "No pending decision to respond to"

        if (pending.playerId != action.playerId) {
            return "You are not the player who needs to make this decision"
        }

        if (pending.id != action.response.decisionId) {
            return "Decision ID mismatch: expected ${pending.id}, got ${action.response.decisionId}"
        }

        return DecisionValidators.validate(pending, action.response, state)
    }

    override fun execute(state: GameState, action: SubmitDecision): ExecutionResult {
        val pending = state.pendingDecision
            ?: return ExecutionResult.error(state, "No pending decision")

        val submittedEvent = DecisionSubmittedEvent(
            pending.id,
            action.playerId,
            description = if ((state.peekContinuation() as? Suspension)?.answer is SecretChoiceContinuation) null
                else buildDecisionDescription(state, pending, action.response)
        )

        // ContinuationHandler consumes the question and answer together by popping its suspension,
        // then drains the automatic work the answer uncovered. Triggers, state-based actions and
        // the waiting-trigger queue are the settle boundary's job, whether the work finished or
        // asked another question.
        val result = continuationHandler.resume(state, action.response)
        if (result.error != null) return result
        val events = listOf(submittedEvent) + result.events
        if (result.pendingDecision != null) return ExecutionResult.propagatePause(result.state, events)

        // The answer finished the work that asked it. If that work carried the game into another
        // step (an untap or cleanup choice, the last opening-hand leyline), the step machine has
        // already handed priority to the active player. Otherwise the answering player gets it.
        // After a resolution that is not always CR 117.3b's active player; see the open bug on
        // priority landing on the last decision's answerer.
        val finishedInStep = result.state.turnNumber == state.turnNumber && result.state.step == state.step
        val answer = (state.continuationStack.lastOrNull() as? Suspension)?.answer
        val finishedBlockerPiles = answer is BlockerPilesContinuation || answer is BlockerPileRestrictionChoiceContinuation
        val settled = if (finishedInStep && !finishedBlockerPiles) result.state.withPriority(action.playerId) else result.state
        return ExecutionResult.success(settled, events)
    }

    /**
     * Build a human-readable description of the decision that was made.
     */
    private fun buildDecisionDescription(
        state: GameState,
        pending: PendingDecision,
        response: DecisionResponse
    ): String? {
        val sourceName = pending.context.sourceName
        val sourcePrefix = sourceName?.let { "($it) " } ?: ""

        return when {
            pending is YesNoDecision && response is YesNoResponse -> {
                val choice = if (response.choice) "Yes" else "No"
                "${sourcePrefix}Chose $choice"
            }

            pending is BatchYesNoDecision && response is BatchYesNoResponse -> {
                val choice = if (response.choice) "Yes" else "No"
                val scope = if (response.applyToAll) " to all ${pending.count}" else ""
                "${sourcePrefix}Chose $choice$scope"
            }

            pending is ChooseNumberDecision && response is NumberChosenResponse -> {
                "${sourcePrefix}Chose X = ${response.number}"
            }

            pending is ChooseColorDecision && response is ColorChosenResponse -> {
                "${sourcePrefix}Chose ${response.color.name.lowercase()}"
            }

            pending is ChooseModeDecision && response is ModesChosenResponse -> {
                val modeTexts = response.selectedModes.mapNotNull { idx ->
                    pending.modes.find { it.index == idx }?.text
                }
                if (modeTexts.isNotEmpty()) {
                    "${sourcePrefix}Chose mode: ${modeTexts.joinToString(", ")}"
                } else null
            }

            pending is ChooseOptionDecision && response is OptionChosenResponse -> {
                val optionText = pending.options.getOrNull(response.optionIndex)
                if (optionText != null) {
                    "${sourcePrefix}Chose $optionText"
                } else null
            }

            pending is ChooseTargetsDecision && response is TargetsResponse -> {
                // A face-down permanent or spell has no name (CR 708.2a / 708.4) — naming it here
                // would leak a morphed or disguised card the moment somebody targeted it.
                val targetNames = response.selectedTargets.values.flatten().mapNotNull { targetId ->
                    state.getEntity(targetId)?.get<CardComponent>()?.name
                        ?.let { nameVisibleToAll(state, targetId, it) }
                        ?: if (state.turnOrder.contains(targetId)) "player" else null
                }
                if (targetNames.isNotEmpty()) {
                    "${sourcePrefix}Targeting ${targetNames.joinToString(", ")}"
                } else null
            }

            pending is DistributeDecision && response is DistributionResponse -> {
                val parts = response.distribution.mapNotNull { (targetId, amount) ->
                    val name = state.getEntity(targetId)?.get<CardComponent>()?.name
                        ?.let { nameVisibleToAll(state, targetId, it) }
                        ?: if (state.turnOrder.contains(targetId)) "player" else null
                    name?.let { "$amount to $it" }
                }
                if (parts.isNotEmpty()) {
                    "${sourcePrefix}Distributed: ${parts.joinToString(", ")}"
                } else null
            }

            // SelectCards, OrderObjects, SearchLibrary, etc. - these produce their own
            // events (ScryCompleted, PermanentsSacrificed, etc.) so no extra log needed
            else -> null
        }
    }

    companion object {
        fun create(services: EngineServices): SubmitDecisionHandler {
            return SubmitDecisionHandler(services.continuationHandler)
        }
    }
}
