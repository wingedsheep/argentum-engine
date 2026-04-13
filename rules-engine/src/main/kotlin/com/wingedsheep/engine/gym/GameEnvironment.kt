package com.wingedsheep.engine.gym

import com.wingedsheep.engine.ai.DecisionResponder
import com.wingedsheep.engine.ai.GameSimulator
import com.wingedsheep.engine.ai.SimulationResult
import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.ai.evaluation.CompositeBoardEvaluator
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.ai.evaluation.CardAdvantage
import com.wingedsheep.engine.ai.evaluation.LifeDifferential
import com.wingedsheep.engine.ai.evaluation.Tempo
import com.wingedsheep.engine.ai.evaluation.ThreatAssessment
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * A stateful game environment for AI agents, MCTS, and reinforcement learning.
 *
 * Wraps the rules engine's immutable [GameState] + [ActionProcessor] into a
 * mutable environment with a simple `reset` / `step` / `legalActions` API.
 *
 * ## Key properties
 *
 * - **Forkable for MCTS:** [fork] creates a new environment pointing at the same
 *   immutable [GameState]. Since state is never mutated in place, forking is free.
 * - **Decision-aware:** When an action pauses for a [PendingDecision], the environment
 *   exposes it via [pendingDecision] and [decisionOptions]. The caller submits a
 *   [SubmitDecision] action through [step] to continue.
 * - **No server dependencies:** Runs entirely within the rules-engine module.
 *
 * ## Usage
 *
 * ```kotlin
 * val env = GameEnvironment.create(cardRegistry)
 * env.reset(GameConfig(
 *     players = listOf(PlayerConfig("Alice", deck1), PlayerConfig("Bob", deck2)),
 *     skipMulligans = true
 * ))
 *
 * while (!env.isTerminal) {
 *     val actions = env.legalActions()
 *     val chosen = actions.random() // or policy network, MCTS, etc.
 *     val result = env.step(chosen.action)
 * }
 *
 * println("Winner: ${env.winnerId}")
 * ```
 *
 * ## MCTS usage
 *
 * ```kotlin
 * fun mcts(env: GameEnvironment, iterations: Int): LegalAction {
 *     val actions = env.legalActions()
 *     val scores = DoubleArray(actions.size)
 *     repeat(iterations) { i ->
 *         val idx = i % actions.size
 *         val child = env.fork()
 *         child.step(actions[idx].action)
 *         scores[idx] += rollout(child)
 *     }
 *     return actions[scores.indices.maxBy { scores[it] }]
 * }
 *
 * fun rollout(env: GameEnvironment): Double {
 *     val playerId = env.agentToAct ?: return env.evaluate(env.playerIds[0])
 *     while (!env.isTerminal) {
 *         val actions = env.legalActions()
 *         env.step(actions.random().action)
 *     }
 *     return env.evaluate(playerId)
 * }
 * ```
 */
class GameEnvironment private constructor(
    private val cardRegistry: CardRegistry,
    private val processor: ActionProcessor,
    private val enumerator: LegalActionEnumerator,
    private val evaluator: BoardEvaluator,
    private val simulator: GameSimulator
) {
    // =========================================================================
    // State
    // =========================================================================

    /** Current immutable game state. */
    var state: GameState = GameState()
        private set

    /** Player entity IDs in turn order, set after [reset]. */
    var playerIds: List<EntityId> = emptyList()
        private set

    /** Cumulative events since the last [reset]. */
    var events: List<GameEvent> = emptyList()
        private set

    /** Events from the most recent [step] call. */
    var lastStepEvents: List<GameEvent> = emptyList()
        private set

    /** Total number of actions submitted since [reset]. */
    var stepCount: Int = 0
        private set

    // =========================================================================
    // Queries
    // =========================================================================

    /** True if the game has ended (someone won, drew, or decked out). */
    val isTerminal: Boolean get() = state.gameOver

    /** The winner's entity ID, or null if the game is ongoing or a draw. */
    val winnerId: EntityId? get() = state.winnerId

    /** The player who currently needs to act (has priority or a pending decision). */
    val agentToAct: EntityId?
        get() = state.pendingDecision?.playerId ?: state.priorityPlayerId

    /** Non-null when the engine is paused waiting for a player decision. */
    val pendingDecision: PendingDecision? get() = state.pendingDecision

    /** Current turn number. */
    val turnNumber: Int get() = state.turnNumber

    // =========================================================================
    // Core API
    // =========================================================================

    /**
     * Initialize a new game and return the opening state.
     *
     * Replaces any existing game state. Set [GameConfig.skipMulligans] to `true`
     * for training runs where you don't want the mulligan phase.
     */
    fun reset(config: GameConfig): StepResult {
        val initializer = GameInitializer(cardRegistry)
        val initResult = initializer.initializeGame(config)
        state = initResult.state
        playerIds = initResult.playerIds
        events = initResult.events
        lastStepEvents = initResult.events
        stepCount = 0
        return buildStepResult(initResult.events)
    }

    /**
     * Submit an action and advance the game state.
     *
     * This processes the action through the engine and auto-resolves trivial
     * decisions (single-target, forced selections, mana autopay). It stops when:
     * - A non-trivial [PendingDecision] needs player input
     * - Priority returns to a player with meaningful choices
     * - The game ends
     *
     * @param action Any [GameAction] — typically a [LegalAction.action] from [legalActions],
     *               or a [SubmitDecision] when responding to a [pendingDecision].
     * @return [StepResult] with the new state, events, rewards, and termination flag.
     * @throws IllegalStateException if the game hasn't been started via [reset].
     */
    fun step(action: GameAction): StepResult {
        check(playerIds.isNotEmpty()) { "Call reset() before step()" }

        val simResult = if (action is SubmitDecision) {
            simulator.simulateDecision(state, action.response)
        } else {
            simulator.simulate(state, action)
        }

        state = simResult.state
        events = events + simResult.events
        lastStepEvents = simResult.events
        stepCount++

        return buildStepResult(simResult.events)
    }

    /**
     * Enumerate all legal actions for the player who needs to act.
     *
     * Uses [EnumerationMode.ACTIONS_ONLY] to skip expensive auto-tap preview
     * computation (not needed for AI/MCTS).
     *
     * If a [PendingDecision] is active, returns an empty list — use
     * [decisionOptions] instead to get the available decision responses,
     * or construct a [SubmitDecision] manually.
     *
     * @return Legal actions for [agentToAct], or empty if no one has priority.
     */
    fun legalActions(): List<LegalAction> {
        val playerId = agentToAct ?: return emptyList()
        if (state.pendingDecision != null) return emptyList()
        if (state.gameOver) return emptyList()
        return enumerator.enumerate(state, playerId, EnumerationMode.ACTIONS_ONLY)
    }

    /**
     * Evaluate the current board state from a player's perspective.
     *
     * Uses the engine's built-in composite evaluator (life, board presence,
     * card advantage, threats, tempo). Returns [Double.MAX_VALUE]/2 for wins,
     * -[Double.MAX_VALUE]/2 for losses.
     *
     * For terminal states, prefer checking [winnerId] directly.
     *
     * @param playerId The player whose perspective to evaluate from.
     * @return A score where higher is better for [playerId].
     */
    fun evaluate(playerId: EntityId): Double {
        return evaluator.evaluate(state, state.projectedState, playerId)
    }

    /**
     * Create a new environment forked from the current state.
     *
     * Since [GameState] is immutable, this is essentially free — the new
     * environment references the same state object. Mutations via [step]
     * on either environment are independent.
     *
     * This is the primary mechanism for MCTS tree expansion.
     */
    fun fork(): GameEnvironment {
        val forked = GameEnvironment(cardRegistry, processor, enumerator, evaluator, simulator)
        forked.state = state
        forked.playerIds = playerIds
        forked.events = emptyList() // forked environments start with clean event history
        forked.lastStepEvents = emptyList()
        forked.stepCount = stepCount
        return forked
    }

    /**
     * Get the terminal reward for each player.
     *
     * - Win = +1.0
     * - Loss = -1.0
     * - Draw / ongoing = 0.0
     */
    fun terminalRewards(): Map<EntityId, Double> {
        if (!isTerminal) return playerIds.associateWith { 0.0 }
        return playerIds.associateWith { pid ->
            when (winnerId) {
                pid -> 1.0
                null -> 0.0 // draw
                else -> -1.0
            }
        }
    }

    // =========================================================================
    // Convenience
    // =========================================================================

    /**
     * Play a complete game with the given action selectors.
     *
     * Each selector maps a player ID to a function that picks an action from the
     * legal action list, or picks a decision response for pending decisions.
     * For players not in [agents], the built-in heuristic AI is used.
     *
     * @param config Game configuration.
     * @param agents Per-player action selectors.
     * @param maxSteps Safety limit to prevent infinite games (default 2000).
     * @return Final [StepResult] with terminal rewards.
     */
    fun playGame(
        config: GameConfig,
        agents: Map<EntityId, ActionSelector> = emptyMap(),
        maxSteps: Int = 2000
    ): StepResult {
        reset(config)

        while (!isTerminal && stepCount < maxSteps) {
            val player = agentToAct ?: break
            val selector = agents[player]

            val action = if (pendingDecision != null) {
                val decision = pendingDecision!!
                val response = selector?.respondToDecision(state, decision)
                    ?: defaultDecisionResponder.respond(state, decision, player)
                SubmitDecision(player, response)
            } else {
                val actions = legalActions()
                if (actions.isEmpty()) break
                selector?.selectAction(state, actions) ?: actions.first { it.affordable }.action
            }

            step(action)
        }

        return buildStepResult(lastStepEvents)
    }

    // =========================================================================
    // Internal
    // =========================================================================

    private val defaultDecisionResponder: DecisionResponder by lazy {
        DecisionResponder(simulator, evaluator)
    }

    private fun buildStepResult(stepEvents: List<GameEvent>): StepResult {
        return StepResult(
            state = state,
            events = stepEvents,
            reward = terminalRewards(),
            terminated = isTerminal,
            truncated = false,
            agentToAct = agentToAct,
            pendingDecision = pendingDecision,
            info = StepInfo(
                turnNumber = turnNumber,
                stepCount = stepCount,
                winnerId = winnerId,
                phase = state.phase,
                step = state.step
            )
        )
    }

    companion object {
        /**
         * Create a new [GameEnvironment] with the default evaluator.
         *
         * @param cardRegistry Registry containing all card definitions to be used.
         * @param evaluator Board evaluator for [evaluate] calls. Defaults to the
         *                  engine's composite evaluator (life, board, cards, threats, tempo).
         */
        fun create(
            cardRegistry: CardRegistry,
            evaluator: BoardEvaluator = defaultEvaluator()
        ): GameEnvironment {
            val services = EngineServices(cardRegistry)
            val processor = ActionProcessor(services, computeUndo = false)
            val enumerator = LegalActionEnumerator.create(cardRegistry)
            val simulator = GameSimulator(cardRegistry, processor, enumerator)
            return GameEnvironment(cardRegistry, processor, enumerator, evaluator, simulator)
        }

        /**
         * Default composite evaluator matching [com.wingedsheep.engine.ai.AIPlayer.defaultEvaluator].
         */
        fun defaultEvaluator(): BoardEvaluator = CompositeBoardEvaluator(
            listOf(
                1.0 to LifeDifferential,
                1.5 to BoardPresence,
                1.0 to CardAdvantage,
                1.2 to ThreatAssessment,
                0.6 to Tempo
            )
        )
    }
}
