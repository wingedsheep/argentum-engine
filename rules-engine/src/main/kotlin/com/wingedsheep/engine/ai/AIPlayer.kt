package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.advisor.CardAdvisorModule
import com.wingedsheep.engine.ai.advisor.CardAdvisorRegistry
import com.wingedsheep.engine.ai.evaluation.*
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * A complete AI player that can play a full game of Magic: The Gathering.
 *
 * Compose from the three layers:
 * - [GameSimulator] — "what happens if I do X?"
 * - [BoardEvaluator] — "how good is this state for me?"
 * - [Strategist] — "which action should I take?"
 * - [DecisionResponder] — "how do I answer this decision?"
 *
 * Usage:
 * ```
 * val ai = AIPlayer.create(cardRegistry, playerId)
 * val action = ai.chooseAction(state)
 * val result = processor.process(state, action)
 * // if paused:
 * val response = ai.respondToDecision(result.state, result.pendingDecision!!)
 * ```
 */
class AIPlayer(
    val playerId: EntityId,
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val strategist: Strategist,
    private val responder: DecisionResponder
) {
    /**
     * Choose the best action from the current legal actions.
     * Returns the [GameAction] to submit to the [ActionProcessor].
     */
    fun chooseAction(state: GameState): GameAction {
        val legalActions = simulator.getLegalActions(state, playerId)
        return chooseFrom(state, legalActions).action
    }

    /**
     * Choose the best [LegalAction] from the given list.
     */
    fun chooseFrom(state: GameState, legalActions: List<LegalAction>): LegalAction {
        return strategist.chooseAction(state, legalActions, playerId)
    }

    /**
     * Respond to a pending decision.
     */
    fun respondToDecision(state: GameState, decision: PendingDecision): DecisionResponse {
        return responder.respond(state, decision, playerId)
    }

    /**
     * Play a full turn step: choose action, submit it, handle any decisions,
     * repeat until priority passes or the game ends.
     *
     * Returns the final state after all AI actions this priority window.
     */
    fun playPriorityWindow(state: GameState, processor: ActionProcessor): GameState {
        var current = state
        var iterations = 0
        val maxIterations = 100

        while (!current.gameOver && iterations < maxIterations) {
            // Handle pending decisions first
            if (current.pendingDecision != null) {
                val decision = current.pendingDecision
                if (decision.playerId != playerId) break // not our decision
                val response = respondToDecision(current, decision)
                val result = processor.process(current, SubmitDecision(playerId, response)).result
                if (result.error != null) break
                current = result.state
                iterations++
                continue
            }

            // Check if we have priority
            if (current.priorityPlayerId != playerId) break

            // Choose and execute an action
            val action = chooseAction(current)
            val result = processor.process(current, action).result
            if (result.error != null) {
                // Action was illegal — submit a safe fallback action
                val fallback = when {
                    current.step == Step.DECLARE_ATTACKERS && current.activePlayerId == playerId -> {
                        // Include mandatory attackers to avoid rejection
                        val legalActions = simulator.getLegalActions(current, playerId)
                        val attackAction = legalActions.find { it.actionType == "DeclareAttackers" }
                        val mandatory = attackAction?.mandatoryAttackers ?: emptyList()
                        val opponentId = current.turnOrder.firstOrNull { it != playerId }
                        val attackerMap = if (mandatory.isNotEmpty() && opponentId != null) {
                            mandatory.associateWith { opponentId }
                        } else emptyMap()
                        DeclareAttackers(playerId, attackerMap)
                    }
                    current.step == Step.DECLARE_BLOCKERS && current.activePlayerId != playerId -> {
                        // Include mandatory blockers to avoid rejection
                        val legalActions = simulator.getLegalActions(current, playerId)
                        val blockerAction = legalActions.find { it.actionType == "DeclareBlockers" }
                        val mandatory = blockerAction?.mandatoryBlockerAssignments ?: emptyMap()
                        val blockerMap = mandatory.mapValues { (_, targets) ->
                            if (targets.isNotEmpty()) listOf(targets.first()) else emptyList()
                        }
                        DeclareBlockers(playerId, blockerMap)
                    }
                    else -> PassPriority(playerId)
                }
                val fallbackResult = processor.process(current, fallback).result
                if (fallbackResult.error != null) break
                current = fallbackResult.state
                iterations++
                break
            }
            current = result.state
            iterations++

            // If we chose to pass priority, stop
            if (action is PassPriority) break
        }

        return current
    }

    companion object {
        /**
         * Create an AI player with the default evaluation weights.
         *
         * @param advisorModules Per-set card advisor modules. Each module registers
         *   [CardAdvisor]s that override generic AI behavior for specific cards.
         */
        fun create(
            cardRegistry: CardRegistry,
            playerId: EntityId,
            advisorModules: List<CardAdvisorModule> = emptyList()
        ): AIPlayer {
            val advisorRegistry = CardAdvisorRegistry()
            advisorModules.forEach { it.register(advisorRegistry) }

            val simulator = GameSimulator(cardRegistry)
            val evaluator = defaultEvaluator()
            val combatAdvisor = CombatAdvisor(simulator, evaluator, cardRegistry, advisorRegistry)
            val responder = DecisionResponder(simulator, evaluator, advisorRegistry = advisorRegistry)

            // Wire up the decision resolver so simulations can resolve non-trivial
            // decisions (modal spells, gift choices, etc.) instead of returning
            // NeedsDecision with an unresolved board state.
            simulator.decisionResolver = { state, decision ->
                responder.respond(state, decision, decision.playerId)
            }

            return AIPlayer(
                playerId = playerId,
                simulator = simulator,
                evaluator = evaluator,
                strategist = Strategist(simulator, evaluator, combatAdvisor = combatAdvisor, advisorRegistry = advisorRegistry),
                responder = responder
            )
        }

        /**
         * Default evaluator: weighted combination of strategic dimensions.
         *
         * Weights reflect how much each dimension contributes to winning:
         * - Board presence is king — creatures win games
         * - Threat assessment catches lethal-on-board situations the other features miss
         * - Life matters more when it's low (handled by non-linear scaling inside the feature)
         * - Card advantage is a long-term edge
         * - Tempo (mana development) matters early but less late
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
