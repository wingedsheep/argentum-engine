package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LeylineDecisionContinuation
import com.wingedsheep.engine.core.LeylinePhaseContinuation
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice

/**
 * Resumes [LeylineDecisionContinuation] frames: the per-card yes/no walk through every
 * player's opening hand that runs once mulligans and bottoming are done.
 *
 * On each resume the resumer:
 *  1. Pops the leyline card off the deciding player's `pendingLeylineCardIds` list.
 *  2. If the player answered yes, routes the card from hand to battlefield through
 *     [ZoneTransitionService] so the standard zone-change pipeline (controller assignment,
 *     [com.wingedsheep.engine.handlers.effects.PermanentEntryTracker], ETB replacements
 *     from other on-battlefield permanents, ZoneChangeEvent emission) fires, then pauses for the
 *     card's own [EntersWithChoice] replacement if it has one (Leyline of Transformation:
 *     "As this enchantment enters, choose a creature type").
 *  3. Looks for the next leyline decision via [com.wingedsheep.engine.handlers.MulliganHandler.getNextLeylineChoice].
 *     If one exists, pauses with the next [com.wingedsheep.engine.core.YesNoDecision]; otherwise
 *     returns success, leaving the state at `step = UNTAP` with no pending decision so that
 *     `SubmitDecisionHandler` advances into the first turn via `turnManager.advanceStep`.
 *
 * Step 3 also runs from the auto-resumed [LeylinePhaseContinuation], which step 2 parks beneath
 * the as-enters choice so the walk survives that pause.
 */
class LeylineContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule, AutoResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(LeylineDecisionContinuation::class, ::resumeLeylineDecision)
    )

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(LeylinePhaseContinuation::class) { state, _, events, checkForMore ->
            continueLeylinePhase(state, events, checkForMore)
        }
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
        val events = mutableListOf<GameEvent>()

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

            val entersChoicePause = pauseForEntersWithChoice(
                newState, continuation.playerId, continuation.leylineCardId, transition.events
            )
            if (entersChoicePause != null) return entersChoicePause
        }

        return continueLeylinePhase(newState, events, checkForMore)
    }

    /**
     * A leyline that just entered from the opening hand still makes its own "as this enters,
     * choose …" choice (CR 614.12) — the card is on the battlefield, but the chosen value has to be
     * recorded before anything reads it. Reuses the shared on-battlefield entry seam
     * ([PermanentEntryReplacements.pauseForEntersWithChoice], also used by played lands and
     * definition-minted tokens), whose resumer stores the value, chains to any further choice, and
     * fires the entry's ETB triggers off a synthesized [ZoneChangeEvent].
     *
     * Because that resumer owns the entry triggers, [transitionEvents] is forwarded *without* the
     * entry [ZoneChangeEvent] — `SubmitDecisionHandler` runs trigger detection over a paused
     * resume's events, so carrying it would fire every enters-the-battlefield trigger twice.
     *
     * A [LeylinePhaseContinuation] is parked beneath the choice so the walk over the remaining
     * leylines resumes once the choice (and any chained choice) resolves.
     *
     * @return the paused result, or `null` when the card has no as-enters choice (or it can't be
     *   presented) and the walk should simply continue.
     */
    private fun pauseForEntersWithChoice(
        state: GameState,
        playerId: EntityId,
        leylineCardId: EntityId,
        transitionEvents: List<GameEvent>
    ): ExecutionResult? {
        val cardComponent = state.getEntity(leylineCardId)?.get<CardComponent>() ?: return null
        val cardDef = services.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        val firstChoice = cardDef.script.replacementEffects
            .filterIsInstance<EntersWithChoice>()
            .sortedBy { it.choiceType.ordinal }
            .firstOrNull() ?: return null

        val parkedState = state.pushContinuation(
            LeylinePhaseContinuation(decisionId = "leyline-phase-${leylineCardId.value}")
        )
        return PermanentEntryReplacements.pauseForEntersWithChoice(
            state = parkedState,
            entityId = leylineCardId,
            controllerId = playerId,
            cardComponent = cardComponent,
            choice = firstChoice,
            fromZone = Zone.HAND,
            carryEvents = transitionEvents.filterNot {
                it is ZoneChangeEvent && it.entityId == leylineCardId
            },
            cardNameOptions = if (firstChoice.choiceType == ChoiceType.CARD_NAME) {
                services.cardRegistry.cardNamesIn(firstChoice.cardNamePool).toList()
            } else emptyList(),
        )
    }

    /**
     * Ask the next player's leyline yes/no, or finish the phase. Shared by the yes/no resumer and
     * by the [LeylinePhaseContinuation] auto-resume that picks the walk back up after an as-enters
     * choice interrupted it.
     */
    private fun continueLeylinePhase(
        state: GameState,
        events: List<GameEvent>,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val nextLeyline = services.mulliganHandler.getNextLeylineChoice(state)
        if (nextLeyline != null) {
            val (nextPlayerId, nextCardId) = nextLeyline
            val nextDecision = services.mulliganHandler.createLeylineDecision(state, nextPlayerId, nextCardId)
            if (nextDecision != null) {
                val (decision, nextContinuation) = nextDecision
                val pausedState = state.pushContinuation(nextContinuation).withPendingDecision(decision)
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
        return checkForMore(state, events)
    }
}
