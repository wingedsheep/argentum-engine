package com.wingedsheep.engine.ai

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Wraps the ActionProcessor to let the AI ask "what happens if I do X?"
 *
 * Because GameState is immutable, simulating an action is just calling process()
 * on the same state — no rollback or cleanup needed.
 */
class GameSimulator(
    private val cardRegistry: CardRegistry,
    private val processor: ActionProcessor = ActionProcessor(cardRegistry),
    private val enumerator: LegalActionEnumerator = LegalActionEnumerator.create(cardRegistry)
) {
    /**
     * Simulate an action, returning the resulting state.
     * Trivial decisions (single legal choice) are auto-resolved.
     */
    fun simulate(state: GameState, action: GameAction): SimulationResult {
        val result = processor.process(state, action)
        return resolveTrivia(result)
    }

    /**
     * Simulate a decision response on a paused state.
     */
    fun simulateDecision(state: GameState, response: DecisionResponse): SimulationResult {
        val pending = state.pendingDecision
            ?: return SimulationResult.Illegal(state, emptyList(), "No pending decision")
        val action = SubmitDecision(pending.playerId, response)
        val result = processor.process(state, action)
        return resolveTrivia(result)
    }

    /**
     * Get all legal actions for a player.
     */
    fun getLegalActions(state: GameState, playerId: EntityId): List<LegalAction> {
        return enumerator.enumerate(state, playerId)
    }

    /**
     * Simulate each legal action (1-ply) and return scored outcomes.
     * Actions that require targets are simulated with each valid target.
     */
    fun expandActions(
        state: GameState,
        playerId: EntityId
    ): List<ActionOutcome> {
        val legalActions = getLegalActions(state, playerId)
        return legalActions
            .filter { it.affordable }
            .map { action -> ActionOutcome(action, simulate(state, action.action)) }
    }

    /**
     * Auto-resolve trivial decisions (only one legal choice).
     */
    private fun resolveTrivia(result: ExecutionResult): SimulationResult {
        var current = result
        var allEvents = result.events

        // Auto-resolve decisions with exactly one legal response, up to a depth limit
        var iterations = 0
        while (current.isPaused && iterations < 50) {
            val decision = current.pendingDecision!!
            val trivialResponse = trivialResponseFor(decision) ?: break

            val submitAction = SubmitDecision(decision.playerId, trivialResponse)
            current = processor.process(current.state, submitAction)
            allEvents = allEvents + current.events
            iterations++
        }

        return when {
            current.error != null ->
                SimulationResult.Illegal(current.state, allEvents, current.error!!)
            current.isPaused ->
                SimulationResult.NeedsDecision(current.state, current.pendingDecision!!, allEvents)
            else ->
                SimulationResult.Terminal(current.state, allEvents)
        }
    }

    /**
     * Returns a trivial response if there's exactly one legal choice, null otherwise.
     */
    private fun trivialResponseFor(decision: PendingDecision): DecisionResponse? = when (decision) {
        // Single target, single requirement → auto-select
        is ChooseTargetsDecision -> {
            val allSingle = decision.targetRequirements.all { req ->
                val targets = decision.legalTargets[req.index] ?: emptyList()
                targets.size == 1 && req.minTargets == 1 && req.maxTargets == 1
            }
            if (allSingle) {
                TargetsResponse(
                    decisionId = decision.id,
                    selectedTargets = decision.targetRequirements.associate { req ->
                        req.index to decision.legalTargets[req.index]!!
                    }
                )
            } else null
        }

        // Forced card selection (min == max == options.size)
        is SelectCardsDecision -> {
            if (decision.minSelections == decision.options.size &&
                decision.maxSelections == decision.options.size
            ) {
                CardsSelectedResponse(decision.id, decision.options)
            } else null
        }

        // Damage assignment with defaults
        is AssignDamageDecision -> {
            if (decision.defaultAssignments.isNotEmpty()) {
                DamageAssignmentResponse(decision.id, decision.defaultAssignments)
            } else null
        }

        // Mana sources — always auto-pay
        is SelectManaSourcesDecision -> {
            ManaSourcesSelectedResponse(decision.id, autoPay = true)
        }

        // Single option
        is ChooseOptionDecision -> {
            if (decision.options.size == 1) {
                OptionChosenResponse(decision.id, 0)
            } else null
        }

        // Single color
        is ChooseColorDecision -> {
            if (decision.availableColors.size == 1) {
                ColorChosenResponse(decision.id, decision.availableColors.first())
            } else null
        }

        // Single mode, min==max==1
        is ChooseModeDecision -> {
            val available = decision.modes.filter { it.available }
            if (available.size == 1 && decision.minModes == 1) {
                ModesChosenResponse(decision.id, listOf(available.first().index))
            } else null
        }

        // Number with single valid value
        is ChooseNumberDecision -> {
            if (decision.minValue == decision.maxValue) {
                NumberChosenResponse(decision.id, decision.minValue)
            } else null
        }

        // Single object ordering
        is OrderObjectsDecision -> {
            if (decision.objects.size <= 1) {
                OrderedResponse(decision.id, decision.objects)
            } else null
        }

        // Library reordering with single card
        is ReorderLibraryDecision -> {
            if (decision.cards.size <= 1) {
                OrderedResponse(decision.id, decision.cards)
            } else null
        }

        else -> null
    }
}

/**
 * A legal action paired with its simulated outcome.
 */
data class ActionOutcome(
    val action: LegalAction,
    val result: SimulationResult
)
