package com.wingedsheep.engine.ai

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.EnumerationMode
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
    private val processor: ActionProcessor = ActionProcessor(EngineServices(cardRegistry), computeUndo = false),
    private val enumerator: LegalActionEnumerator = LegalActionEnumerator.create(cardRegistry)
) {
    /**
     * Optional resolver for non-trivial decisions encountered during simulation.
     * Set after constructing the [DecisionResponder] to enable full spell resolution
     * for modal spells, fight spells with gift modes, etc.
     *
     * Without this, simulations that hit a non-trivial decision (e.g., ChooseModeDecision
     * with 2+ modes) return [SimulationResult.NeedsDecision] and the evaluator scores the
     * unresolved state — which makes every modal spell look worse than passing.
     */
    var decisionResolver: ((GameState, PendingDecision) -> DecisionResponse)? = null

    /** Guard against recursive resolution — inner simulations (from DecisionResponder
     *  evaluating alternatives) should NOT re-enter the resolver. */
    private var isResolving = false
    /**
     * Simulate an action and resolve the stack to completion.
     *
     * After executing the action, both players auto-pass priority until
     * the stack is empty (spells resolve) or a non-trivial decision is needed.
     * This ensures the evaluator sees the actual effect of casting a spell,
     * not just "spell on stack, lands tapped".
     */
    fun simulate(state: GameState, action: GameAction): SimulationResult {
        val result = processor.process(state, action)
        return resolveToQuietState(result)
    }

    /**
     * Simulate a decision response on a paused state.
     */
    fun simulateDecision(state: GameState, response: DecisionResponse): SimulationResult {
        val pending = state.pendingDecision
            ?: return SimulationResult.Illegal(state, emptyList(), "No pending decision")
        val action = SubmitDecision(pending.playerId, response)
        val result = processor.process(state, action)
        return resolveToQuietState(result)
    }

    /**
     * Get all legal actions for a player.
     */
    fun getLegalActions(state: GameState, playerId: EntityId): List<LegalAction> {
        return enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
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
     * Resolve to a "quiet" state: auto-pass priority for both players and
     * auto-resolve trivial decisions until the stack is empty or a real
     * decision is needed.
     *
     * Without this, simulating CastSpell would leave the spell on the stack
     * (lands tapped, creature not yet on battlefield), making every spell
     * look worse than passing.
     */
    private fun resolveToQuietState(result: ExecutionResult): SimulationResult {
        var current = result
        var allEvents = result.events
        var iterations = 0
        val maxIterations = 100

        while (iterations < maxIterations) {
            if (current.error != null) {
                return SimulationResult.Illegal(current.state, allEvents, current.error)
            }

            // Auto-resolve trivial decisions; use decisionResolver for non-trivial ones
            if (current.isPaused) {
                val decision = current.pendingDecision!!
                val trivialResponse = trivialResponseFor(decision)
                if (trivialResponse != null) {
                    val submitAction = SubmitDecision(decision.playerId, trivialResponse)
                    current = processor.process(current.state, submitAction)
                    allEvents = allEvents + current.events
                    iterations++
                    continue
                }
                // Non-trivial decision: try the pluggable resolver (but not recursively —
                // inner simulations from DecisionResponder evaluating alternatives break here)
                val resolver = decisionResolver
                if (resolver != null && !isResolving) {
                    try {
                        isResolving = true
                        val response = resolver(current.state, decision)
                        val submitAction = SubmitDecision(decision.playerId, response)
                        current = processor.process(current.state, submitAction)
                        allEvents = allEvents + current.events
                        iterations++
                    } finally {
                        isResolving = false
                    }
                    continue
                }
                break
            }

            // Stack is non-empty — auto-pass priority for whoever has it
            // to let spells resolve. This simulates both players choosing not
            // to respond, which is the most common outcome.
            val state = current.state
            if (state.stack.isNotEmpty() && state.priorityPlayerId != null && !state.gameOver) {
                val passAction = PassPriority(state.priorityPlayerId)
                current = processor.process(state, passAction)
                allEvents = allEvents + current.events
                iterations++
                continue
            }

            // Stack empty, no pending decision — we've reached a quiet state
            break
        }

        return when {
            current.error != null ->
                SimulationResult.Illegal(current.state, allEvents, current.error)
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
