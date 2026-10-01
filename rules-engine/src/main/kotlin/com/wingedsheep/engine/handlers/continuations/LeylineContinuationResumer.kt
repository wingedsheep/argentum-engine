package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LeylineDecisionContinuation
import com.wingedsheep.engine.core.LeylinePhaseContinuation
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
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
import com.wingedsheep.sdk.scripting.effects.Effect

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
 *     advances from UNTAP into the first turn via `turnManager.advanceStep`, the same call the
 *     mulligan handlers make when no one has a leyline.
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

        val revealEffect = openingHandRevealOf(newState, continuation.leylineCardId)
        if (response.choice && revealEffect != null) {
            return revealFromOpeningHand(newState, continuation, revealEffect, checkForMore)
        }

        if (response.choice) {
            // Route the card to the battlefield through the standard zone-change pipeline.
            // Owner == controller for leyline starts; the card must already exist with its
            // CardComponent + OwnerComponent set (it does — it was instantiated at init).
            val transition = services.zones.moveToZone(
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
     * entry [ZoneChangeEvent]. The settle boundary runs trigger detection over a paused resume's
     * events, so carrying it would fire every enters-the-battlefield trigger twice.
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
            LeylinePhaseContinuation
        )
        return PermanentEntryReplacements.pauseForEntersWithChoice(
            state = parkedState,
            entityId = leylineCardId,
            controllerId = playerId,
            cardComponent = cardComponent,
            choice = firstChoice,
            fromZone = Zone.HAND,
            entryOldObject = transitionEvents.filterIsInstance<ZoneChangeEvent>().firstOrNull { it.entityId == leylineCardId }?.oldObject,
            entryNewObject = transitionEvents.filterIsInstance<ZoneChangeEvent>().firstOrNull { it.entityId == leylineCardId }?.newObject,
            carryEvents = transitionEvents.filterNot {
                it is ZoneChangeEvent && it.entityId == leylineCardId
            },
            cardNameOptions = if (firstChoice.choiceType == ChoiceType.CARD_NAME) {
                services.cardRegistry.cardNamesIn(firstChoice.cardNamePool).toList()
            } else emptyList(),
        )
    }

    /** The card's opening-hand reveal payoff, or null when its opening-hand action is a leyline start. */
    private fun openingHandRevealOf(state: GameState, cardId: EntityId): Effect? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val script = services.cardRegistry.getCard(cardComponent.cardDefinitionId)?.script ?: return null
        return script.openingHandReveal?.takeUnless { script.mayStartOnBattlefield }
    }

    /**
     * "You may reveal this card from your opening hand. If you do, …" (CR 103.6b): reveal the card
     * to every player — it stays in hand — then run the card's payoff with the card as source and
     * its owner as controller. The payoff is normally a delayed trigger, which CR 603.7a lets a
     * player action create.
     *
     * A [LeylinePhaseContinuation] is parked beneath the payoff so that, should it ever pause for a
     * decision, the opening-hand walk resumes once it finishes; when it completes synchronously
     * the park is popped again and the walk continues inline.
     */
    private fun revealFromOpeningHand(
        state: GameState,
        continuation: LeylineDecisionContinuation,
        effect: Effect,
        checkForMore: CheckForMore
    ): ExecutionResult {
        val card = state.getEntity(continuation.leylineCardId)?.get<CardComponent>()
        val revealed = CardsRevealedEvent(
            revealingPlayerId = continuation.playerId,
            cardIds = listOf(continuation.leylineCardId),
            cardNames = listOf(continuation.cardName),
            imageUris = listOf(card?.imageUri),
            source = continuation.cardName,
            revealToSelf = false
        )
        val context = EffectContext(
            sourceId = continuation.leylineCardId,
            controllerId = continuation.playerId
        )
        val result = services.effectExecutorRegistry
            .execute(state.pushContinuation(LeylinePhaseContinuation), effect, context)
            .toExecutionResult()
        val events = listOf<GameEvent>(revealed) + result.events
        if (result.outcome is Outcome.Paused) return result.copy(events = events)
        if (result.outcome is Outcome.Rejected) return result
        val (_, unparked) = result.state.popContinuation()
        return continueLeylinePhase(unparked, events, checkForMore)
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
            val result = services.mulliganHandler.createLeylineDecision(state, nextPlayerId, nextCardId)
            if (result != null) {
                return ExecutionResult.propagatePause(result.state, events + result.events)
            }
        }

        // No more leyline prompts: the game begins, exactly as it does when no one has a leyline.
        return mergeAndContinue(services.turnManager.advanceStep(state), events, checkForMore)
    }
}
