package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.TriggerModalModeSelectionContinuation
import com.wingedsheep.engine.core.TriggerModalTargetSelectionContinuation
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Drives mode + target selection for a modal **triggered** ability on its way to the stack
 * (CR 603.3c) — the trigger-time twin of [CastModalContinuationResumer].
 *
 * 1. [TriggerModalModeSelectionContinuation] iterates until `chooseCount` modes are picked (or
 *    "Done" is chosen once `minChooseCount` is met).
 * 2. [TriggerModalTargetSelectionContinuation] then collects one mode's targets per resume.
 * 3. The last step puts the ability on the stack with `chosenModes` / `modeTargetsOrdered`
 *    populated, which is what emits the `BecomesTargetEvent`s ward keys on.
 *
 * Neither pause is cancellable: unlike a cast, a trigger is already on its way to the stack and
 * the player has no legal way to back out. Both hand off to `checkForMore` afterwards so any
 * sibling triggers waiting on a [com.wingedsheep.engine.core.PendingTriggersContinuation] still
 * reach the stack.
 */
class ModalTriggerContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(TriggerModalModeSelectionContinuation::class, ::resumeModeSelection),
        resumer(TriggerModalTargetSelectionContinuation::class, ::resumeTargetSelection)
    )

    private fun resumeModeSelection(
        state: GameState,
        continuation: TriggerModalModeSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for modal trigger mode selection")
        }

        val totalOptions = continuation.offeredIndices.size + (if (continuation.doneOptionOffered) 1 else 0)
        if (response.optionIndex < 0 || response.optionIndex >= totalOptions) {
            return ExecutionResult.error(state, "Invalid mode option index: ${response.optionIndex}")
        }

        val chooseDone = continuation.doneOptionOffered &&
            response.optionIndex == continuation.offeredIndices.size
        val chosenIndex = if (chooseDone) null else continuation.offeredIndices[response.optionIndex]
        val newSelected = continuation.selectedModeIndices + listOfNotNull(chosenIndex)

        if (!chooseDone && newSelected.size < continuation.chooseCount) {
            // Another pick to make. Narrow the remaining pool unless modes may repeat.
            val nextAvailable = if (continuation.allowRepeat) null else {
                (continuation.availableIndices ?: continuation.modes.indices.toList())
                    .filter { it != chosenIndex }
            }
            return services.triggerProcessor.presentTriggerModalModeDecision(
                state = state,
                ability = continuation.ability,
                outerTargets = continuation.outerTargets,
                outerTargetRequirements = continuation.outerTargetRequirements,
                modal = ModalEffect(
                    modes = continuation.modes,
                    chooseCount = continuation.chooseCount,
                    minChooseCount = continuation.minChooseCount,
                    allowRepeat = continuation.allowRepeat
                ),
                selectedModeIndices = newSelected,
                availableIndices = nextAvailable,
                causedByAttack = continuation.causedByAttack
            ).thenCheckForMore(checkForMore)
        }

        return services.triggerProcessor.presentTriggerModalTargetDecision(
            state = state,
            ability = continuation.ability,
            outerTargets = continuation.outerTargets,
            outerTargetRequirements = continuation.outerTargetRequirements,
            modes = continuation.modes,
            chosenModeIndices = newSelected,
            resolvedModeTargets = emptyList(),
            currentOrdinal = 0,
            causedByAttack = continuation.causedByAttack
        ).thenCheckForMore(checkForMore)
    }

    private fun resumeTargetSelection(
        state: GameState,
        continuation: TriggerModalTargetSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for modal trigger target selection")
        }

        // Keep per-requirement order so buildNamedTargets maps each target to its own slot.
        val chosenTargets = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityIdToChosenTarget(state, it) } }

        return services.triggerProcessor.presentTriggerModalTargetDecision(
            state = state,
            ability = continuation.ability,
            outerTargets = continuation.outerTargets,
            outerTargetRequirements = continuation.outerTargetRequirements,
            modes = continuation.modes,
            chosenModeIndices = continuation.chosenModeIndices,
            resolvedModeTargets = continuation.resolvedModeTargets + listOf(chosenTargets),
            currentOrdinal = continuation.currentOrdinal + 1,
            causedByAttack = continuation.causedByAttack
        ).thenCheckForMore(checkForMore)
    }

    /**
     * Once the ability is finally on the stack (or fizzled for lack of legal modes), let the
     * continuation chain drain — sibling triggers queued behind this one still need placing.
     */
    private fun ExecutionResult.thenCheckForMore(checkForMore: CheckForMore): ExecutionResult =
        if (isSuccess) checkForMore(state, events) else this
}
