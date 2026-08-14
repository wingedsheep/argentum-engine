package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CostPaymentContinuation
import com.wingedsheep.engine.core.CostPaymentManaSelectionContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.mechanics.cost.CostPaymentContext
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.cost.PaymentResult
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Resumer for [CostPaymentContinuation] — the single resume path for every [PayCost] variant paid
 * through [CostPaymentService].
 *
 * It reads the player's response (yes/no, card selection, or option pick depending on the cost
 * shape), decides paid vs. declined, delegates the actual mutation to
 * [CostPaymentService.performPayment], then runs the continuation's `onPaid` / `onDeclined` follow-up
 * and chains via `checkForMore` so any caller-pushed frame beneath resumes too.
 */
class CostPaymentContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    private val paymentService = CostPaymentService(services)

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(CostPaymentContinuation::class, ::resume),
        resumer(CostPaymentManaSelectionContinuation::class, ::resumeManaSelection)
    )

    fun resume(
        state: GameState,
        continuation: CostPaymentContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult = when (val cost = continuation.cost) {
        is PayCost.Choice -> resumeChoice(state, continuation, cost, response, checkForMore)
        // Resolved away before the frame is built; should never reach the resumer.
        is PayCost.OwnManaCost ->
            ExecutionResult.error(state, "OwnManaCost should have been resolved before payment")
        is PayCost.Atom -> when (val atom = cost.atom) {
            // Yes/no costs: mana, life, mill (the milled cards are the top of the library, so
            // there is nothing to select), and random discard.
            is CostAtom.Mana, is CostAtom.PayLife, is CostAtom.Mill ->
                resumeYesNo(state, continuation, cost, response, checkForMore)
            is CostAtom.Discard ->
                if (atom.random) resumeYesNo(state, continuation, cost, response, checkForMore)
                else resumeSelection(state, continuation, cost, response, checkForMore)
            // Selection costs.
            is CostAtom.ExileFrom, is CostAtom.RevealFromHand, is CostAtom.Sacrifice,
            is CostAtom.ReturnToHand, is CostAtom.TapPermanents,
            // Collect evidence is a selection cost too — the sum gate rides on the decision's
            // `minTotalManaValue`, so resuming it is the ordinary selection path.
            is CostAtom.CollectEvidence ->
                resumeSelection(state, continuation, cost, response, checkForMore)
            is CostAtom.RemoveCounters ->
                if (atom.self || atom.counterType == null) {
                    resumeYesNo(state, continuation, cost, response, checkForMore)
                } else {
                    resumeSelection(state, continuation, cost, response, checkForMore)
                }
            // Ability-scoped only; never reaches a PayCost prompt, but it is a yes/no shape
            // (nothing to select) if one is ever built.
            is CostAtom.PutCountersOnSelf ->
                resumeYesNo(state, continuation, cost, response, checkForMore)
            // VariablePermanents is an activated-ability-only cost, never a PayCost — unreachable here.
            is CostAtom.VariablePermanents ->
                ExecutionResult.error(state, "VariablePermanents is not a payable cost in this context")
        }
    }

    private fun resumeYesNo(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is YesNoResponse) {
            return ExecutionResult.error(state, "Expected yes/no response for cost payment")
        }
        if (!response.choice) return declined(state, continuation, checkForMore)

        // A mana cost gets a second step: which sources to tap. Ward and "counter unless you pay"
        // have always worked this way; going straight to the solver here meant the payer couldn't
        // choose, and couldn't activate a mana ability to cover a cost the solver can't auto-tap
        // (CR 605.3a — see [ManaPaymentWindow]).
        manaSourceWindow(state, continuation, cost)?.let { return it }

        val execution = paymentService.performPayment(state, continuation.payerId, cost, continuation.sourceId, emptyMap())
        // A defensive payment failure (e.g. mana solve came up short) falls through to declined.
        return if (execution.success) paid(execution.state, execution.events, continuation, checkForMore)
        else declined(state, continuation, checkForMore)
    }

    /**
     * Raises the mana-source window for a [cost] the payer just agreed to, or returns null when the
     * cost isn't mana or their floating mana already covers it (nothing to choose).
     */
    private fun manaSourceWindow(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost
    ): ExecutionResult? {
        val manaCost = ((cost as? PayCost.Atom)?.atom as? CostAtom.Mana)?.cost ?: return null
        if (ManaPaymentWindow.floatingManaCovers(state, continuation.payerId, manaCost)) return null

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ManaPaymentWindow.buildDecision(
            state = state,
            playerId = continuation.payerId,
            cost = manaCost,
            decisionId = decisionId,
            prompt = "Pay $manaCost",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            canDecline = true,
            cardRegistry = services.cardRegistry
        )
        val frame = CostPaymentManaSelectionContinuation(
            decisionId = decisionId,
            inner = continuation,
            manaCost = manaCost,
            availableSources = decision.availableSources
        )
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(frame),
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.payerId,
                    decisionType = "SELECT_MANA_SOURCES",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Applies the payer's source picks, then finishes the payment [manaSourceWindow] interrupted.
     * Declining here is the same outcome as answering "no" to the original prompt.
     */
    private fun resumeManaSelection(
        state: GameState,
        continuation: CostPaymentManaSelectionContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is ManaSourcesSelectedResponse) {
            return ExecutionResult.error(state, "Expected mana sources selected response for cost payment")
        }
        val inner = continuation.inner
        val floated = ManaPaymentWindow.floatSelectedMana(
            state, inner.payerId, continuation.manaCost, response, continuation.availableSources, services
        )
        if (!floated.paid) return declined(floated.state, inner, checkForMore)

        val execution = paymentService.performPayment(
            floated.state, inner.payerId, inner.cost, inner.sourceId, emptyMap()
        )
        return if (execution.success) {
            paid(execution.state, floated.events + execution.events, inner, checkForMore)
        } else {
            declined(floated.state, inner, checkForMore)
        }
    }

    private fun resumeSelection(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is CardsSelectedResponse) {
            return ExecutionResult.error(state, "Expected card-selection response for cost payment")
        }
        val selected = response.selectedCards.groupingBy { it }.eachCount()
        val atom = (cost as? PayCost.Atom)?.atom
        val selectedCount = if (atom is CostAtom.RemoveCounters && atom.counterType != null) {
            selected.values.sum()
        } else {
            response.selectedCards.size
        }
        if (selectedCount < paymentService.requiredCount(cost)) {
            return declined(state, continuation, checkForMore)
        }
        // "Sacrifice N ... with different names" — reject selections that repeat a name (CR 601.2g costs
        // must be paid in full and legally). The chosen permanents must be pairwise distinctly named.
        if (atom is CostAtom.Sacrifice && atom.distinctNames &&
            !CostPaymentService.allDistinctNames(state, response.selectedCards)
        ) {
            return declined(state, continuation, checkForMore)
        }
        val execution = paymentService.performPayment(state, continuation.payerId, cost, continuation.sourceId, selected)
        return if (execution.success) paid(execution.state, execution.events, continuation, checkForMore)
        else declined(state, continuation, checkForMore)
    }

    private fun resumeChoice(
        state: GameState,
        continuation: CostPaymentContinuation,
        cost: PayCost.Choice,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option-choice response for cost payment")
        }
        // The trailing option (index == options.size) is "Don't pay".
        if (response.optionIndex >= cost.options.size) {
            return declined(state, continuation, checkForMore)
        }
        // Re-enter the service with the chosen sub-cost, carrying the same follow-up. It pushes a
        // fresh CostPaymentContinuation and pauses again — handling a sub-cost that itself needs input.
        val chosen = cost.options[response.optionIndex]
        val result = paymentService.pay(state, continuation.payerId, chosen, continuation.sourceId, contextOf(continuation))
        return when (result) {
            is PaymentResult.Pending -> ExecutionResult.paused(result.state, result.pendingDecision, result.events)
            // canAfford was checked when building the option list, so a sub-cost should be payable;
            // treat any unexpected non-pending result as a decline so the punisher branch still runs.
            else -> declined(result.state, continuation, checkForMore)
        }
    }

    private fun paid(
        state: GameState,
        priorEvents: List<GameEvent>,
        continuation: CostPaymentContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult = runFollowup(state, priorEvents, continuation.onPaid, continuation, checkForMore)

    private fun declined(
        state: GameState,
        continuation: CostPaymentContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult = runFollowup(state, emptyList(), continuation.onDeclined, continuation, checkForMore)

    private fun runFollowup(
        state: GameState,
        priorEvents: List<GameEvent>,
        followup: Effect?,
        continuation: CostPaymentContinuation,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (followup == null) return checkForMore(state, priorEvents)
        val result = services.effectExecutorRegistry
            .execute(state, followup, effectContext(state, continuation))
            .toExecutionResult()
        val allEvents = priorEvents + result.events
        return if (result.isPaused) {
            ExecutionResult.paused(result.state, result.pendingDecision!!, allEvents)
        } else {
            checkForMore(result.state, allEvents)
        }
    }

    private fun effectContext(state: GameState, continuation: CostPaymentContinuation): EffectContext =
        EffectContext(
            sourceId = continuation.sourceId,
            controllerId = continuation.payerId,
            targets = continuation.targets,
            pipeline = PipelineState(
                namedTargets = continuation.namedTargets,
                storedCollections = continuation.storedCollections
            )
        )

    private fun contextOf(continuation: CostPaymentContinuation): CostPaymentContext =
        CostPaymentContext(
            onPaid = continuation.onPaid,
            onDeclined = continuation.onDeclined,
            targets = continuation.targets,
            namedTargets = continuation.namedTargets,
            storedCollections = continuation.storedCollections
        )
}
