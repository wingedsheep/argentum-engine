package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.continuations.*
import com.wingedsheep.engine.state.GameState

/**
 * Handles resumption of execution after a player decision.
 *
 * The top suspension retains the player's question and the operation that consumes its answer.
 * This handler validates the response identity, consumes that suspension, and dispatches the
 * answer operation together with its question.
 *
 * Delegates to specialized resumer modules via the ContinuationResumerRegistry.
 */
class ContinuationHandler(
    private val services: EngineServices
) {
    private val effectRunner = EffectContinuationRunner(services.effectExecutorRegistry)

    private val registry = ContinuationResumerRegistry().apply {
        val forcedPlayResumer = ForcedPlayResumer(services)
        registerModule(forcedPlayResumer)
        registerAutoResumerModule(forcedPlayResumer)
        registerAutoResumerModule(ManaAbilitySourcesResumer())
        registerAutoResumerModule(ManaSpendingObligationsResumer())
        registerAutoResumerModule(ScopedManaProductionResumer(services))
        // Core engine resumers
        registerModule(EffectAndTriggerContinuationResumer(services, effectRunner))
        registerModule(MiscContinuationResumer(services, effectRunner))

        // Core engine auto-resumers
        registerAutoResumerModule(CoreAutoResumerModule(services, effectRunner))

        // Specialized resumer modules
        registerModule(CombatContinuationResumer(services))
        registerModule(CombatTaxContinuationResumer(services))
        registerModule(ColorChoiceContinuationResumer(services, effectRunner))
        val chainResumer = ChainSpellContinuationResumer(services)
        registerModule(chainResumer)
        registerAutoResumerModule(chainResumer)
        registerModule(CreatureTypeChoiceContinuationResumer(services))
        registerModule(TextReplacementContinuationResumer(services))
        registerModule(DrawReplacementContinuationResumer(services))
        registerModule(CardSpecificContinuationResumer(services))
        registerModule(DiscardAndDrawContinuationResumer(services))
        registerModule(StateBasedContinuationResumer(services))
        registerModule(SacrificeAndPayContinuationResumer(services))
        registerModule(CollectEvidenceContinuationResumer(services.zones))
        registerModule(CostPaymentContinuationResumer(services))
        registerModule(ManaPaymentContinuationResumer(services))
        registerModule(LibraryAndZoneContinuationResumer(services, targetFinder = services.targetFinder))
        registerModule(GuessContinuationResumer(services))
        registerModule(RedistributeLifeContinuationResumer(services))
        registerModule(ModalAndCloneContinuationResumer(services))
        registerModule(RoomDoorContinuationResumer(services))
        registerModule(CastModalContinuationResumer(services))
        registerModule(ModalTriggerContinuationResumer(services))
        registerModule(TokenContinuationResumer(services))
        registerModule(RingTemptContinuationResumer(services))
        registerModule(AmassContinuationResumer(services))
        val leylineResumer = LeylineContinuationResumer(services)
        registerModule(leylineResumer)
        registerAutoResumerModule(leylineResumer)
        registerModule(ActivateAbilityXCostContinuationResumer(services))
        registerModule(ActivateAbilityOpponentTargetResumer(services))

        // Replacement effect system
        val replacementResumer = ReplacementContinuationResumer(
            services.replacementEffectProcessor,
            services
        )
        registerModule(replacementResumer)
        registerAutoResumerModule(replacementResumer)
    }

    /** Continuation types with a registered resumer, for `ContinuationResumerCoverageTest`. */
    fun registeredAnswerTypes() = registry.registeredAnswerTypes()

    /** Automatic continuation types with a registered auto-resumer, for the same coverage test. */
    fun registeredAutomaticTypes() = registry.registeredAutomaticTypes()

    /**
     * Resume execution after a decision is submitted.
     *
     * @param state The game state containing the suspension being answered
     * @param response The player's decision response
     * @return The result of resuming execution
     */
    fun resume(state: GameState, response: DecisionResponse): ExecutionResult =
        resumeWithin(state, response, 0)

    /**
     * Resume nested work without consuming the caller's continuation prefix. Mana planning uses
     * this boundary so production can finish while the enclosing payment and resolution stay live.
     */
    fun resumeWithin(state: GameState, response: DecisionResponse, continuationFloor: Int): ExecutionResult {
        require(continuationFloor >= 0)
        if (state.continuationStack.size <= continuationFloor) {
            return ExecutionResult.error(state, if (continuationFloor == 0)
                "No suspension is awaiting an answer" else "No suspension above the continuation boundary")
        }
        val continueWithin: CheckForMore = { next, events ->
            checkForMoreContinuations(next, events, continuationFloor)
        }
        val suspension = state.peekContinuation() as? Suspension
            ?: return ExecutionResult.error(state, "No suspension is awaiting an answer")
        if (suspension.question.id != response.decisionId) {
            return ExecutionResult.error(
                state,
                "Decision ID mismatch: expected ${suspension.question.id}, got ${response.decisionId}"
            )
        }

        val (_, stateAfterPop) = state.popContinuation()
        val result = registry.resume(stateAfterPop, suspension.answer, suspension.question, response, continueWithin)
        // Casting resumers can finish a local picker without draining enclosing work.
        // A mandatory play must then retry the card or resume the original resolution.
        val completed = if (result.outcome is Outcome.Done &&
            result.state.continuationStack.any { it is FinishForcedPlayContinuation }) {
            continueWithin(result.state, result.events)
        } else result
        // Any resumed effect tree can produce mana and pause again before the activation boundary.
        val productions = completed.state.continuationStack.filterIsInstance<ScopedManaProductionContinuation>()
        return if (productions.isEmpty()) completed else completed.copy(events = completed.events.filterNot { event ->
            event is ManaAddedEvent && productions.any {
                it.sourceId == event.sourceId && it.playerId == event.playerId
            }
        })
    }

    /**
     * Drain the automatic work a resumer uncovered, then report where execution ended up.
     *
     * `pendingDecision` is derived from the stack, so a resumer that installed a suspension and
     * handed control back here leaves one on top that no auto-resumer will match. Reporting that
     * as success would hide a live question: the caller runs SBAs and returns priority, the client
     * is never asked, and the orphaned suspension makes every later `pushContinuation` throw. Read
     * the state rather than trusting the caller to have propagated the pause itself.
     */
    private fun checkForMoreContinuations(
        state: GameState,
        events: List<GameEvent>,
        continuationFloor: Int = 0,
    ): ExecutionResult {
        if (continuationFloor > 0 && state.continuationStack.size <= continuationFloor) {
            return ExecutionResult.success(state, events)
        }
        val continueWithin: CheckForMore = { next, nextEvents ->
            checkForMoreContinuations(next, nextEvents, continuationFloor)
        }
        // A resumer that dealt damage or countered a spell outside the effect registry (a declined
        // "counter unless you pay", a divided-damage answer) may owe a replacement's rest — Guile's
        // free cast, Vigor's counters. Run it before the interrupted resolution goes on, as the
        // registry would have. A rider that asks a question stacks its suspension above the
        // remaining frames, which resume once it's answered.
        if (state.pendingReplacementRiders.isNotEmpty() && state.pendingDecision == null) {
            val drained = com.wingedsheep.engine.replacement.ReplacementRiders.drain(
                state, services.effectExecutorRegistry::execute
            )
            if (drained.outcome is com.wingedsheep.engine.core.Outcome.Paused) {
                return ExecutionResult.propagatePause(drained.state, events + drained.events)
            }
            return continueWithin(drained.state, events + drained.events)
        }
        registry.tryAutoResume(state, events, continueWithin)?.let { return it }
        return if (state.pendingDecision != null) ExecutionResult.propagatePause(state, events)
        else ExecutionResult.success(state, events)
    }
}
