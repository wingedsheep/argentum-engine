package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LeylineDecisionContinuation
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Zone

/**
 * Resumes [LeylineDecisionContinuation] frames: the per-card yes/no walk through every
 * player's opening hand that runs once mulligans and bottoming are done.
 *
 * On each resume the resumer:
 *  1. Pops the leyline card off the deciding player's `pendingLeylineCardIds` list.
 *  2. If the player answered yes, routes the card from hand to battlefield through
 *     [ZoneTransitionService] so the standard zone-change pipeline (controller assignment,
 *     [com.wingedsheep.engine.handlers.effects.PermanentEntryTracker], ETB replacements
 *     from other on-battlefield permanents, ZoneChangeEvent emission) fires.
 *  3. Looks for the next leyline decision via [com.wingedsheep.engine.handlers.MulliganHandler.getNextLeylineChoice].
 *     If one exists, pauses with the next [com.wingedsheep.engine.core.YesNoDecision]; otherwise
 *     returns success, leaving the state at `step = UNTAP` with no pending decision so that
 *     `SubmitDecisionHandler` advances into the first turn via `turnManager.advanceStep`.
 */
class LeylineContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(LeylineDecisionContinuation::class, ::resumeLeylineDecision)
    )

    private fun resumeLeylineDecision(
        state: GameState,
        continuation: LeylineDecisionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for leyline decision")
        }

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Drop this card from the deciding player's pending list — whether the player said
        // yes or no, the choice for this specific card is resolved.
        val mullState = newState.getEntity(continuation.playerId)?.get<MulliganStateComponent>()
        if (mullState != null) {
            val updated = mullState.copy(
                pendingLeylineCardIds = mullState.pendingLeylineCardIds.filter { it != continuation.leylineCardId }
            )
            newState = newState.updateEntity(continuation.playerId) { container ->
                container.with(updated)
            }
        }

        if (response.choice) {
            // Route the card to the battlefield through the standard zone-change pipeline.
            // Owner == controller for leyline starts; the card must already exist with its
            // CardComponent + OwnerComponent set (it does — it was instantiated at init).
            val transition = ZoneTransitionService.moveToZone(
                state = newState,
                entityId = continuation.leylineCardId,
                destinationZone = Zone.BATTLEFIELD,
                options = ZoneEntryOptions(controllerId = continuation.playerId)
            )
            newState = transition.state
            events.addAll(transition.events)
        }

        // After applying this choice, look for the next leyline prompt in turn order.
        val nextLeyline = services.mulliganHandler.getNextLeylineChoice(newState)
        if (nextLeyline != null) {
            val (nextPlayerId, nextCardId) = nextLeyline
            val nextDecision = services.mulliganHandler.createLeylineDecision(newState, nextPlayerId, nextCardId)
            if (nextDecision != null) {
                val (decision, nextContinuation) = nextDecision
                val pausedState = newState.pushContinuation(nextContinuation).withPendingDecision(decision)
                return ExecutionResult.paused(
                    pausedState,
                    decision,
                    events + DecisionRequestedEvent(
                        decisionId = decision.id,
                        playerId = nextPlayerId,
                        decisionType = "YES_NO",
                        prompt = decision.prompt
                    )
                )
            }
        }

        // No more leyline prompts. Let SubmitDecisionHandler's "step == UNTAP, no pending"
        // branch fire turnManager.advanceStep to start turn 1.
        return checkForMore(newState, events)
    }
}
