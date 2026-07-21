package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ActivateAbilityOpponentTargetContinuation
import com.wingedsheep.engine.core.ActivateAbilityOpponentChooserContinuation
import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser

/**
 * Resumes an activated ability after an opponent has chosen the target(s) for its
 * "… of an opponent's choice" requirement (Cuombajj Witches: "{T}: This creature deals 1 damage to
 * any target and 1 damage to any target of an opponent's choice").
 *
 * [ActivateAbilityHandler.execute] paused before paying any cost and raised a
 * [com.wingedsheep.engine.core.ChooseTargetsDecision] for the opponent over the opponent-chosen
 * requirements only. Here we:
 *
 * 1. Convert the opponent's [TargetsResponse] into [ChosenTarget]s (per-requirement order).
 * 2. Interleave them with the controller's already-chosen targets according to the full requirement
 *    list (script order), so [com.wingedsheep.engine.handlers.EffectContext.buildNamedTargets] maps
 *    each target to the right requirement when the ability resolves.
 * 3. Re-enter the handler with the merged targets and `opponentTargetsChosen = true`, so cost
 *    payment and stack placement run exactly once and the opponent-target pause is not re-raised.
 *
 * Cancellation pops the frame and restores priority to the controller with no side effects — the
 * pause happened before any cost was paid.
 */
class ActivateAbilityOpponentTargetResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    private val handler: ActivateAbilityHandler by lazy { ActivateAbilityHandler.create(services) }

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ActivateAbilityOpponentChooserContinuation::class, ::resumeOpponentChooser),
        resumer(ActivateAbilityOpponentTargetContinuation::class, ::resumeOpponentTargets)
    )

    private fun resumeOpponentChooser(
        state: GameState,
        continuation: ActivateAbilityOpponentChooserContinuation,
        response: DecisionResponse,
        @Suppress("UNUSED_PARAMETER") checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            return ExecutionResult.success(state.withPriority(continuation.action.playerId))
        }
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option response for opponent chooser")
        }
        val deciderId = continuation.opponentIds.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid opponent choice")

        return handler.pauseForOpponentChosenTargetsForDecider(
            state = state,
            action = continuation.action,
            sourceName = continuation.sourceName,
            fullTargetReqs = continuation.fullRequirements,
            opponentReqs = continuation.opponentRequirements,
            deciderId = deciderId
        )
    }

    private fun resumeOpponentTargets(
        state: GameState,
        continuation: ActivateAbilityOpponentTargetContinuation,
        response: DecisionResponse,
        @Suppress("UNUSED_PARAMETER") checkForMore: CheckForMore
    ): ExecutionResult {
        if (response is CancelDecisionResponse) {
            // Pause happened before any cost was paid — safe to bail with no mutations.
            return ExecutionResult.success(state.withPriority(continuation.action.playerId))
        }
        if (response !is TargetsResponse) {
            return ExecutionResult.error(state, "Expected targets response for opponent's-choice target selection")
        }

        // The opponent's chosen targets, in requirement order (requirement index ascending).
        val opponentTargets: List<ChosenTarget> = response.selectedTargets.entries
            .sortedBy { it.key }
            .flatMap { (_, ids) -> ids.map { entityIdToChosenTarget(state, it) } }

        // The opponent's pick was already checked against the offered legal targets and per-
        // requirement count bounds upstream in DecisionValidators.validateTargets (run by
        // SubmitDecisionHandler before this resumer). Re-assert the minimum total here as cheap
        // defense-in-depth against a malformed response reaching the resume path.
        val expectedCount = continuation.opponentRequirements.sumOf { it.effectiveMinCount }
        if (opponentTargets.size < expectedCount) {
            return ExecutionResult.error(state, "Not enough targets chosen for opponent's choice")
        }

        // The interleave below relies on each requirement consuming exactly `count` targets
        // (the positional model buildNamedTargets uses on resolution). That holds only for
        // fixed-count requirements; an optional/variable requirement would misalign the cursors.
        // ActivateAbilityHandler.pauseForOpponentChosenTargets already rejects non-fixed-count
        // shapes before raising the decision; re-assert here as defense-in-depth so a wrong target
        // mapping can never be produced on the resume path.
        if (continuation.fullRequirements.any { it.minCount != it.count || it.optional || it.unlimited }) {
            return ExecutionResult.error(
                state,
                "Opponent-chosen targets are only supported with fixed-count requirements"
            )
        }

        // Interleave the controller's targets (already on the action) with the opponent's, in the
        // script order of the full requirement list. Each requirement consumes `count` targets.
        val controllerTargets = continuation.action.targets
        val merged = mutableListOf<ChosenTarget>()
        var controllerCursor = 0
        var opponentCursor = 0
        for (req in continuation.fullRequirements) {
            val take = req.count
            val source = if (req.chooser == TargetChooser.Opponent) {
                val end = (opponentCursor + take).coerceAtMost(opponentTargets.size)
                opponentTargets.subList(opponentCursor, end).also { opponentCursor = end }
            } else {
                val end = (controllerCursor + take).coerceAtMost(controllerTargets.size)
                controllerTargets.subList(controllerCursor, end).also { controllerCursor = end }
            }
            merged.addAll(source)
        }

        val replay = continuation.action.copy(
            targets = merged.toList(),
            opponentTargetsChosen = true
        )
        return handler.execute(state, replay)
    }
}
