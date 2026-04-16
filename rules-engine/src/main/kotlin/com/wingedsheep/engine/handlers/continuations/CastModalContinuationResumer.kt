package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.CastModalModeSelectionContinuation
import com.wingedsheep.engine.core.CastModalTargetSelectionContinuation
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent

/**
 * Resumes the cast-time mode and target selection flow for choose-N modal spells
 * (rules 601.2b–c, 700.2). Works together with the modal-pause emitted by
 * [CastSpellHandler.execute]:
 *
 * 1. Mode selection ([CastModalModeSelectionContinuation]) iterates until the player
 *    has picked `chooseCount` modes or chosen "Done" after `minChooseCount`.
 * 2. Target selection ([CastModalTargetSelectionContinuation]) iterates per chosen
 *    mode that has target requirements.
 * 3. Once complete, delegates back into [CastSpellHandler] with a fully populated
 *    [com.wingedsheep.engine.core.CastSpell] so cost payment and stack placement run
 *    exactly once (no partial state is persisted during the pauses).
 *
 * Cancellation pops the continuation and restores priority with no side effects —
 * this is safe because the pause happens before any cost is paid.
 */
class CastModalContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    private val castSpellHandler: CastSpellHandler by lazy { CastSpellHandler.create(services) }

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CastModalModeSelectionContinuation::class, ::resumeCastModalModeSelection),
        resumer(CastModalTargetSelectionContinuation::class, ::resumeCastModalTargetSelection)
    )

    fun resumeCastModalModeSelection(
        state: GameState,
        continuation: CastModalModeSelectionContinuation,
        response: DecisionResponse,
        @Suppress("UNUSED_PARAMETER") checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            // Pause happened before any cost was paid — safe to bail without mutations.
            return ExecutionResult.success(state.withPriority(continuation.casterId))
        }
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for modal cast-time mode selection")
        }

        val totalOptions = continuation.offeredIndices.size + (if (continuation.doneOptionOffered) 1 else 0)
        if (response.optionIndex < 0 || response.optionIndex >= totalOptions) {
            return ExecutionResult.error(state, "Invalid mode option index: ${response.optionIndex}")
        }

        val chooseDone = continuation.doneOptionOffered &&
            response.optionIndex == continuation.offeredIndices.size

        val newSelected = if (chooseDone) continuation.selectedModeIndices
        else continuation.selectedModeIndices + continuation.offeredIndices[response.optionIndex]

        val cardName = state.getEntity(continuation.cardId)?.get<CardComponent>()?.name ?: "modal spell"

        // More modes still needed?
        val reachedMax = newSelected.size >= continuation.chooseCount
        if (!chooseDone && !reachedMax) {
            val nextAvailable: List<Int>? = if (continuation.allowRepeat) {
                null
            } else {
                val chosenIndex = continuation.offeredIndices[response.optionIndex]
                (continuation.availableIndices ?: continuation.offeredIndices).filter { it != chosenIndex }
            }
            val repeatAvailable = if (continuation.allowRepeat) continuation.offeredIndices else null

            // Rebuild a fresh ModalEffect shim for the next pause. We can recover the
            // effect shape from the continuation's carried [modes] list + counts.
            val modalShim = com.wingedsheep.sdk.scripting.effects.ModalEffect(
                modes = continuation.modes,
                chooseCount = continuation.chooseCount,
                minChooseCount = continuation.minChooseCount,
                allowRepeat = continuation.allowRepeat
            )
            return castSpellHandler.presentCastModalModeDecision(
                state = state,
                cardId = continuation.cardId,
                casterId = continuation.casterId,
                cardName = cardName,
                baseCastAction = continuation.baseCastAction,
                modalEffect = modalShim,
                selectedModeIndices = newSelected,
                availableIndices = nextAvailable,
                repeatAvailableIndices = repeatAvailable
            )
        }

        // All modes picked — transition to target selection (skipping modes with no
        // target requirements) or finalize directly.
        return castSpellHandler.presentCastModalTargetDecision(
            state = state,
            cardId = continuation.cardId,
            casterId = continuation.casterId,
            cardName = cardName,
            baseCastAction = continuation.baseCastAction,
            modes = continuation.modes,
            chosenModeIndices = newSelected,
            resolvedModeTargets = emptyList(),
            currentOrdinal = 0
        )
    }

    fun resumeCastModalTargetSelection(
        state: GameState,
        continuation: CastModalTargetSelectionContinuation,
        response: DecisionResponse,
        @Suppress("UNUSED_PARAMETER") checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            return ExecutionResult.success(state.withPriority(continuation.casterId))
        }
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for modal cast-time target selection")
        }

        // Flatten selected targets, preserving per-requirement order so buildNamedTargets
        // maps them to the right requirement on resolution.
        val chosenTargets = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityIdToChosenTarget(state, it) } }

        val newResolvedTargets = continuation.resolvedModeTargets + listOf(chosenTargets)
        val nextOrdinal = continuation.currentOrdinal + 1

        val cardName = state.getEntity(continuation.cardId)?.get<CardComponent>()?.name ?: "modal spell"

        return castSpellHandler.presentCastModalTargetDecision(
            state = state,
            cardId = continuation.cardId,
            casterId = continuation.casterId,
            cardName = cardName,
            baseCastAction = continuation.baseCastAction,
            modes = continuation.modes,
            chosenModeIndices = continuation.chosenModeIndices,
            resolvedModeTargets = newResolvedTargets,
            currentOrdinal = nextOrdinal
        )
    }
}
