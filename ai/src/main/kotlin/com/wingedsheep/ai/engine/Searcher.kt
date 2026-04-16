package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Multi-ply game tree search with alpha-beta pruning and adaptive depth.
 *
 * The search is designed around MTG's priority system:
 * - **Move ordering**: 1-ply scores are computed first, then the top candidates
 *   get deeper search. This makes alpha-beta pruning highly effective.
 * - **Opponent modeling**: When the opponent has no instant-speed responses
 *   (no untapped mana, no free abilities), their ply is skipped entirely.
 * - **Adaptive depth**: Close decisions get deeper search; obvious ones stay at 1-ply.
 * - **Branching cap**: At each ply, only the top N candidates are searched deeper.
 *
 * Depth is measured in "priority passes", not turns. A single MTG turn contains
 * many priority passes (one per phase/step), so depth-2 means "my action + opponent's
 * best response", not "two full turns".
 */
class Searcher(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val config: SearchConfig = SearchConfig()
) {
    private var nodesSearched = 0

    /**
     * Score an action using multi-ply search.
     *
     * @param state Current game state
     * @param action The action being evaluated
     * @param playerId The AI player
     * @param depth How many plies to search (1 = current behavior, 2 = consider opponent response)
     * @return Evaluation score from the AI player's perspective
     */
    fun searchAction(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        depth: Int
    ): Double {
        nodesSearched = 0
        val result = simulator.simulate(state, action.action)
        if (result is SimulationResult.Illegal) return Double.MIN_VALUE / 2

        val resultState = result.state
        if (resultState.gameOver) {
            return evaluator.evaluate(resultState, resultState.projectedState, playerId)
        }

        // Depth 1 = just evaluate the resulting state (no opponent modeling)
        if (depth <= 1) {
            return evaluator.evaluate(resultState, resultState.projectedState, playerId)
        }

        // Depth 2+: model opponent's response
        val opponentId = state.getOpponent(playerId) ?: return evaluator.evaluate(resultState, resultState.projectedState, playerId)

        // Check if opponent can actually respond
        if (!canRespond(resultState, opponentId)) {
            // Opponent will just pass — evaluate as if depth 1 but with one less depth
            // for our next action
            return evaluateQuiet(resultState, playerId, depth - 1)
        }

        // Opponent gets to respond: minimax (opponent minimizes our score)
        return opponentPly(resultState, playerId, opponentId, depth - 1, Double.MIN_VALUE / 2, Double.MAX_VALUE / 2)
    }

    /**
     * Opponent's ply: they choose the action that minimizes our score.
     * Uses alpha-beta pruning.
     */
    private fun opponentPly(
        state: GameState,
        aiPlayerId: EntityId,
        opponentId: EntityId,
        remainingDepth: Int,
        alpha: Double,
        beta: Double
    ): Double {
        if (state.gameOver || nodesSearched >= config.maxNodes) {
            return evaluator.evaluate(state, state.projectedState, aiPlayerId)
        }

        val opponentActions = simulator.getLegalActions(state, opponentId)
            .filter { it.affordable && !it.isManaAbility }

        if (opponentActions.isEmpty()) {
            return evaluator.evaluate(state, state.projectedState, aiPlayerId)
        }

        // If opponent can only pass, they pass
        val nonPass = opponentActions.filter { it.actionType != "PassPriority" }
        if (nonPass.isEmpty()) {
            return evaluator.evaluate(state, state.projectedState, aiPlayerId)
        }

        // Move ordering: quick-evaluate opponent's options, search top candidates
        val ordered = orderMoves(state, nonPass, opponentId)
        val pass = opponentActions.find { it.actionType == "PassPriority" }

        // Passing is always an option for the opponent (baseline)
        var bestForOpponent = if (pass != null) {
            val passResult = simulator.simulate(state, pass.action)
            evaluator.evaluate(passResult.state, passResult.state.projectedState, aiPlayerId)
        } else {
            Double.MAX_VALUE / 2 // no pass means they must act
        }
        var currentBeta = beta.coerceAtMost(bestForOpponent)

        // Search opponent's best moves (they minimize our score)
        val toSearch = ordered.take(config.opponentBranchingCap)
        for (action in toSearch) {
            if (alpha >= currentBeta) break // alpha-beta cutoff
            nodesSearched++

            val result = simulator.simulate(state, action.action)
            if (result is SimulationResult.Illegal) continue

            val score = if (remainingDepth <= 1 || nodesSearched >= config.maxNodes) {
                evaluator.evaluate(result.state, result.state.projectedState, aiPlayerId)
            } else {
                // Our response to their action
                ourPly(result.state, aiPlayerId, remainingDepth - 1, alpha, currentBeta)
            }

            if (score < bestForOpponent) {
                bestForOpponent = score
                currentBeta = currentBeta.coerceAtMost(score)
            }
        }

        return bestForOpponent
    }

    /**
     * Our ply: we choose the action that maximizes our score.
     */
    private fun ourPly(
        state: GameState,
        aiPlayerId: EntityId,
        remainingDepth: Int,
        alpha: Double,
        beta: Double
    ): Double {
        if (state.gameOver || nodesSearched >= config.maxNodes) {
            return evaluator.evaluate(state, state.projectedState, aiPlayerId)
        }

        val actions = simulator.getLegalActions(state, aiPlayerId)
            .filter { it.affordable && !it.isManaAbility }

        if (actions.isEmpty()) {
            return evaluator.evaluate(state, state.projectedState, aiPlayerId)
        }

        val ordered = orderMoves(state, actions, aiPlayerId)
        val pass = actions.find { it.actionType == "PassPriority" }

        var best = if (pass != null) {
            val passResult = simulator.simulate(state, pass.action)
            evaluator.evaluate(passResult.state, passResult.state.projectedState, aiPlayerId)
        } else {
            Double.MIN_VALUE / 2
        }
        var currentAlpha = alpha.coerceAtLeast(best)

        val toSearch = ordered.filter { it.actionType != "PassPriority" }.take(config.ourBranchingCap)
        for (action in toSearch) {
            if (currentAlpha >= beta) break
            nodesSearched++

            val result = simulator.simulate(state, action.action)
            if (result is SimulationResult.Illegal) continue

            val score = if (remainingDepth <= 1 || nodesSearched >= config.maxNodes) {
                evaluator.evaluate(result.state, result.state.projectedState, aiPlayerId)
            } else {
                val opponentId = state.getOpponent(aiPlayerId)
                if (opponentId != null && canRespond(result.state, opponentId)) {
                    opponentPly(result.state, aiPlayerId, opponentId, remainingDepth - 1, currentAlpha, beta)
                } else {
                    evaluateQuiet(result.state, aiPlayerId, remainingDepth - 1)
                }
            }

            if (score > best) {
                best = score
                currentAlpha = currentAlpha.coerceAtLeast(score)
            }
        }

        return best
    }

    /**
     * Evaluate a "quiet" position (opponent can't respond).
     * If we still have depth budget and it's our priority, search our next action.
     */
    private fun evaluateQuiet(state: GameState, playerId: EntityId, remainingDepth: Int): Double {
        if (state.gameOver || remainingDepth <= 0 || nodesSearched >= config.maxNodes) {
            return evaluator.evaluate(state, state.projectedState, playerId)
        }

        // If it's our priority, we can take another action
        if (state.priorityPlayerId == playerId) {
            return ourPly(state, playerId, remainingDepth, Double.MIN_VALUE / 2, Double.MAX_VALUE / 2)
        }

        return evaluator.evaluate(state, state.projectedState, playerId)
    }

    /**
     * Quick heuristic check: can this player meaningfully respond right now?
     *
     * If they have no untapped lands and no free activated abilities, their only
     * option is to pass. This prunes entire plies from the search tree.
     */
    private fun canRespond(state: GameState, playerId: EntityId): Boolean {
        // Not their priority? Can't respond.
        if (state.priorityPlayerId != playerId) return false

        // Quick check: do they have untapped mana sources?
        val hasUntappedMana = state.projectedState.getBattlefieldControlledBy(playerId).any { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@any false
            card.isLand && state.getEntity(entityId)?.has<TappedComponent>() != true
        }

        // Also check if they have cards in hand (to cast instants)
        val hasCardsInHand = state.getZone(playerId, Zone.HAND).isNotEmpty()

        return hasUntappedMana && hasCardsInHand
    }

    /**
     * Order moves by quick 1-ply evaluation for alpha-beta efficiency.
     * Best moves first means more cutoffs.
     */
    private fun orderMoves(state: GameState, actions: List<LegalAction>, playerId: EntityId): List<LegalAction> {
        if (actions.size <= 1) return actions

        return actions.sortedByDescending { action ->
            nodesSearched++
            val result = simulator.simulate(state, action.action)
            if (result is SimulationResult.Illegal) Double.MIN_VALUE / 2
            else evaluator.evaluate(result.state, result.state.projectedState, playerId)
        }
    }

    /**
     * Determine the ideal search depth for this decision.
     */
    fun recommendDepth(
        state: GameState,
        scoredCandidates: List<Pair<LegalAction, Double>>,
        playerId: EntityId
    ): Int {
        if (scoredCandidates.size <= 1) return 1

        val opponentId = state.getOpponent(playerId)

        // Can opponent even respond? If not, deeper search is pointless.
        if (opponentId == null || !canRespond(state, opponentId)) return 1

        val sorted = scoredCandidates.sortedByDescending { it.second }
        val bestScore = sorted[0].second
        val secondScore = sorted[1].second
        val gap = bestScore - secondScore

        val myLife = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 20
        val theirLife = state.getEntity(opponentId)?.get<LifeTotalComponent>()?.life ?: 20

        // Close decision — top 2 within 5% of each other: think harder
        val isCloseCall = gap < (bestScore.coerceAtLeast(1.0) * 0.05).coerceAtLeast(1.0)

        // Low life — mistakes are fatal
        val isLowLife = myLife <= 7 || theirLife <= 7

        // Combat phase — highest variance decisions
        val isCombat = state.phase == Phase.COMBAT

        return when {
            isCloseCall && isLowLife -> config.maxDepth
            isCloseCall || isLowLife -> (config.maxDepth - 1).coerceAtLeast(2)
            isCombat -> 2
            else -> 1
        }
    }
}

/**
 * Configuration for the search algorithm.
 */
data class SearchConfig(
    /** Maximum search depth (in priority passes). 3 = my action → opponent response → my follow-up. */
    val maxDepth: Int = 3,

    /** Max candidate actions to search deeply on our turns. */
    val ourBranchingCap: Int = 6,

    /** Max candidate actions to search for opponent responses. */
    val opponentBranchingCap: Int = 4,

    /** Hard ceiling on total nodes evaluated (prevents runaway search). */
    val maxNodes: Int = 500
)
