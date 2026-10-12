package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState

class ActivationModalContinuationResumer(private val services: EngineServices) : ContinuationResumerModule {
    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ActivationModalModeContinuation::class, ::modes),
        resumer(ActivationModalTargetContinuation::class, ::targets)
    )

    private fun modes(state: GameState, frame: ActivationModalModeContinuation, response: DecisionResponse,
                      @Suppress("UNUSED_PARAMETER") checkForMore: CheckForMore): ExecutionResult {
        if (response is CancelDecisionResponse) return ExecutionResult.success(state.withPriority(frame.action.playerId))
        val pick = (response as? OptionChosenResponse)?.optionIndex
            ?: return ExecutionResult.error(state, "Expected a mode choice")
        val done = frame.doneOffered && pick == frame.offeredIndices.size
        val index = if (done) null else frame.offeredIndices.getOrNull(pick)
            ?: return ExecutionResult.error(state, "Invalid mode choice")
        val selected = frame.selectedIndices + listOfNotNull(index)
        val picker = services.activateAbilityHandler.modalChoices
        return if (done || selected.size == frame.modal.chooseCount)
            picker.presentTargets(state, frame.action, frame.modal, selected)
        else picker.presentModes(state, frame.action, frame.modal, selected)
    }

    private fun targets(state: GameState, frame: ActivationModalTargetContinuation, response: DecisionResponse,
                        @Suppress("UNUSED_PARAMETER") checkForMore: CheckForMore): ExecutionResult {
        if (response is CancelDecisionResponse) return ExecutionResult.success(state.withPriority(frame.action.playerId))
        if (response !is TargetsResponse) return ExecutionResult.error(state, "Expected mode targets")
        val chosen = response.selectedTargets.entries.sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityIdToChosenTarget(state, it) } }
        val groups = frame.action.modeTargetGroupCountsOrdered.toMutableList()
        while (groups.size <= frame.ordinal) groups.add(emptyList())
        groups[frame.ordinal] = frame.modal.modes[frame.selectedIndices[frame.ordinal]].targetRequirements.indices
            .map { response.selectedTargets[it].orEmpty().size }
        return services.activateAbilityHandler.modalChoices.presentTargets(state,
            frame.action.copy(modeTargetGroupCountsOrdered = groups), frame.modal,
            frame.selectedIndices, frame.resolvedTargets + listOf(chosen), frame.ordinal + 1)
    }
}
