package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.sdk.core.Step
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
    private val enumerator: LegalActionEnumerator = LegalActionEnumerator.create(cardRegistry),
    /**
     * Carry a simulation past the empty stack to the end of the combat damage step, when blockers
     * are already declared.
     *
     * A quiet state is "the stack is empty", which inside combat is *before damage*. Three puzzles
     * fail on exactly that gap — `instants-05` (Fog), `activate-05` (firebreathing) and their
     * relatives all pay mana now for something that only materialises when damage is dealt, so the
     * post-simulation board is strictly worse than passing and the AI passes. Phase 7 answers this
     * with full playouts; this answers only the case where the answer is already determined, which
     * is why it costs one step rather than two turns.
     *
     * **Scoped to blockers-already-declared on purpose.** From `DECLARE_ATTACKERS` the outcome
     * still depends on how the defender blocks, and nothing here would declare those blocks — the
     * simulation would either stall or silently score an unblocked alpha strike. Once blocks are
     * in, the only thing between the current state and the damage is the damage.
     *
     * Off for [AiProfile.LEGACY_V0], which has to stay frozen, and off by default so a
     * `GameSimulator` built anywhere else keeps its historical horizon.
     */
    private val resolveThroughCombatDamage: Boolean = false,
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
        val result = processor.process(state, action).result
        return resolveToQuietState(result)
    }

    /**
     * Simulate a decision response on a paused state.
     */
    fun simulateDecision(state: GameState, response: DecisionResponse): SimulationResult {
        val pending = state.pendingDecision
            ?: return SimulationResult.Illegal(state, emptyList(), "No pending decision")
        val action = SubmitDecision(pending.playerId, response)
        val result = processor.process(state, action).result
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
            val error = current.error
            if (error != null) {
                return SimulationResult.Illegal(current.state, allEvents, error)
            }

            // Auto-resolve trivial decisions; use decisionResolver for non-trivial ones
            if (current.isPaused) {
                val decision = current.pendingDecision!!
                val trivialResponse = trivialResponseFor(decision)
                if (trivialResponse != null) {
                    val submitAction = SubmitDecision(decision.playerId, trivialResponse)
                    current = processor.process(current.state, submitAction).result
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
                        current = processor.process(current.state, submitAction).result
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
            val priorityPlayerId = state.priorityPlayerId
            if (state.stack.isNotEmpty() && priorityPlayerId != null && !state.gameOver) {
                val passAction = PassPriority(priorityPlayerId)
                current = processor.process(state, passAction).result
                allEvents = allEvents + current.events
                iterations++
                continue
            }

            // Stack empty, no pending decision — normally a quiet state. Inside combat with
            // blockers already declared it is a *pre-damage* state, and the whole point of the
            // candidate may be the damage; pass priority to advance the step and look again.
            if (resolveThroughCombatDamage && isPreDamageCombatState(state)) {
                if (priorityPlayerId == null || state.gameOver) break
                current = processor.process(state, PassPriority(priorityPlayerId)).result
                allEvents = allEvents + current.events
                iterations++
                continue
            }

            break
        }

        val finalError = current.error
        return when {
            finalError != null ->
                SimulationResult.Illegal(current.state, allEvents, finalError)
            current.isPaused ->
                SimulationResult.NeedsDecision(current.state, current.pendingDecision!!, allEvents)
            else ->
                SimulationResult.Terminal(current.state, allEvents)
        }
    }

    /**
     * Returns a trivial response if there's exactly one legal choice, null otherwise.
     *
     * The rules live in [TrivialDecisions] so a rollout playout answers a forced decision exactly
     * as the simulator does — a playout that diverged here would make rollout scores incomparable
     * with the static ones they replace, for no benefit.
     */
    private fun trivialResponseFor(decision: PendingDecision): DecisionResponse? =
        TrivialDecisions.responseFor(decision)

    /**
     * True while combat damage is still ahead of us and nothing but priority stands in its way.
     *
     * `DECLARE_BLOCKERS` means blocks are in and the damage is next; `FIRST_STRIKE_COMBAT_DAMAGE`
     * means the first-strike half has been dealt and the regular half has not. Reaching
     * `COMBAT_DAMAGE` with an empty stack means the damage is already on the board — the turn-based
     * action happens on entering the step, before anyone gets priority — so that is where we stop.
     *
     * The attacker check keeps an empty combat from costing anything: with nothing attacking there
     * is no damage to wait for, and advancing would only move the evaluation further from the
     * decision being scored.
     */
    private fun isPreDamageCombatState(state: GameState): Boolean {
        if (state.step != Step.DECLARE_BLOCKERS && state.step != Step.FIRST_STRIKE_COMBAT_DAMAGE) {
            return false
        }
        return state.getBattlefield().any { state.getEntity(it)?.has<AttackingComponent>() == true }
    }
}

/**
 * A legal action paired with its simulated outcome.
 */
data class ActionOutcome(
    val action: LegalAction,
    val result: SimulationResult
)
