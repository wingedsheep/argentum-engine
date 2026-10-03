package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.rollout.FastDecisionResponder
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
    /**
     * How many automatic transitions — auto-passed priorities and auto-answered trivial decisions —
     * one `simulate` call may spend before it gives up and reports [SimulationResult.StoppedAtLimit].
     *
     * The shipped value is an order of magnitude above any real resolution; it is a parameter only
     * so a test can reach the guard deterministically instead of building a board that loops.
     */
    private val maxAutomaticTransitions: Int = DEFAULT_MAX_AUTOMATIC_TRANSITIONS,
) {
    init {
        require(maxAutomaticTransitions > 0) { "maxAutomaticTransitions must be positive" }
    }

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
     * The constant-time policy used while [isResolving] blocks the strategic resolver.
     *
     * Only reached from inside a resolver's own simulation, where the choice is between one cheap
     * heuristic answer and abandoning the resolution entirely.
     */
    private val fallbackResponder = FastDecisionResponder()
    /**
     * Simulate an action and resolve the stack to completion.
     *
     * After executing the action, both players auto-pass priority until
     * the stack is empty (spells resolve) or a non-trivial decision is needed.
     * This ensures the evaluator sees the actual effect of casting a spell,
     * not just "spell on stack, lands tapped". If automatic resolution still has work after
     * [maxAutomaticTransitions], the unfinished state is returned as
     * [SimulationResult.StoppedAtLimit], never as successful completion.
     */
    fun simulate(state: GameState, action: GameAction): SimulationResult {
        val play = state.pendingDecision as? PlayCardDecision
        val submission = if (play != null && (action is CastSpell || action is PlayLand)) {
            SubmitDecision(play.playerId, PlayCardResponse(play.id, action))
        } else action
        val result = processor.process(state, submission).result
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

    /** Cheap completion for a forced play when no strategic picker is installed. */
    internal fun completeForcedPlay(state: GameState, playerId: EntityId): GameAction {
        val offer = getLegalActions(state, playerId).first { it.affordable && !it.hasUnfillableTargetRequirement }
        val bound = XCostSelection.bindBestX(state, offer)
        return TargetSelection.fillHeuristically(state, bound, playerId, fillPartialRequirements = true)
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

        while (true) {
            val error = current.error
            if (error != null) {
                return SimulationResult.Illegal(current.state, allEvents, error)
            }

            // Terminal means the simulator has reached its stopping boundary. A real game end is
            // one such boundary and remains distinguishable through GameState.gameOver.
            if (current.state.gameOver) {
                return SimulationResult.Terminal(current.state, allEvents)
            }

            // Auto-resolve trivial decisions; use decisionResolver for non-trivial ones
            if (current.outcome is Outcome.Paused) {
                val decision = current.pendingDecision!!
                val trivialResponse = trivialResponseFor(decision)
                if (trivialResponse != null) {
                    if (iterations >= maxAutomaticTransitions) {
                        return stoppedAtLimit(current, allEvents, iterations)
                    }
                    val submitAction = SubmitDecision(decision.playerId, trivialResponse)
                    current = processor.process(current.state, submitAction).result
                    allEvents = allEvents + current.events
                    iterations++
                    continue
                }
                // Non-trivial decision: hand it to the pluggable resolver.
                val resolver = decisionResolver
                if (resolver != null) {
                    if (iterations >= maxAutomaticTransitions) {
                        return stoppedAtLimit(current, allEvents, iterations)
                    }
                    val response = if (isResolving) {
                        // The strategic resolver scores its alternatives by simulating them, so it
                        // cannot be re-entered from inside one of its own simulations. Answering
                        // with the O(1) rollout policy is what the playout path already does, and
                        // it beats stopping: an abandoned resolution scores a board with the ward
                        // unpaid or the combat damage unassigned — a position the game never
                        // actually reaches.
                        if (decision is PlayCardDecision) {
                            PlayCardResponse(decision.id, completeForcedPlay(current.state, decision.playerId))
                        } else fallbackResponder.respond(current.state, decision, decision.playerId)
                    } else {
                        try {
                            isResolving = true
                            resolver(current.state, decision)
                        } finally {
                            isResolving = false
                        }
                    }
                    val submitAction = SubmitDecision(decision.playerId, response)
                    current = processor.process(current.state, submitAction).result
                    allEvents = allEvents + current.events
                    iterations++
                    continue
                }
                return SimulationResult.NeedsDecision(current.state, decision, allEvents)
            }

            // Stack is non-empty — auto-pass priority for whoever has it
            // to let spells resolve. This simulates both players choosing not
            // to respond, which is the most common outcome.
            val state = current.state
            val priorityPlayerId = state.priorityPlayerId
            if (state.stack.isNotEmpty() && priorityPlayerId != null && !state.gameOver) {
                if (iterations >= maxAutomaticTransitions) {
                    return stoppedAtLimit(current, allEvents, iterations)
                }
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
                if (priorityPlayerId == null || state.gameOver) {
                    return SimulationResult.Terminal(state, allEvents)
                }
                if (iterations >= maxAutomaticTransitions) {
                    return stoppedAtLimit(current, allEvents, iterations)
                }
                current = processor.process(state, PassPriority(priorityPlayerId)).result
                allEvents = allEvents + current.events
                iterations++
                continue
            }

            return SimulationResult.Terminal(state, allEvents)
        }
    }

    private fun stoppedAtLimit(
        current: ExecutionResult,
        events: List<GameEvent>,
        automaticTransitions: Int,
    ): SimulationResult.StoppedAtLimit = SimulationResult.StoppedAtLimit(
        state = current.state,
        events = events,
        automaticTransitions = automaticTransitions,
        limit = maxAutomaticTransitions,
    )

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

    companion object {
        /**
         * The shipped bound on automatic transitions per `simulate` call.
         *
         * Chosen the same way the server's `GameStallGuard` thresholds are: far above anything a
         * real resolution reaches, so only a genuinely stuck automatic resolution ever meets it.
         */
        const val DEFAULT_MAX_AUTOMATIC_TRANSITIONS = 100
    }
}

/**
 * A legal action paired with its simulated outcome.
 */
data class ActionOutcome(
    val action: LegalAction,
    val result: SimulationResult
)
