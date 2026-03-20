package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Chooses which [LegalAction] to take when the AI has priority.
 *
 * Uses 1-ply lookahead: simulate each affordable action, evaluate the
 * resulting board state, and pick the best. Actions are pruned and
 * categorized before simulation to keep the search fast.
 */
class Strategist(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator
) {
    fun chooseAction(
        state: GameState,
        legalActions: List<LegalAction>,
        playerId: EntityId
    ): LegalAction {
        if (legalActions.size == 1) return legalActions.first()

        // Split into affordable non-mana actions vs pass/mana
        val pass = legalActions.find { it.actionType == "PassPriority" }
        val affordable = legalActions.filter { it.affordable && !it.isManaAbility && it.actionType != "PassPriority" }

        // If nothing to do but pass, pass
        if (affordable.isEmpty()) return pass ?: legalActions.first()

        // Prune obviously bad actions
        val candidates = prune(affordable, state, playerId)

        // Score the current state as baseline (what happens if we just pass)
        val passScore = if (pass != null) {
            evaluateAction(state, pass, playerId)
        } else {
            evaluator.evaluate(state, state.projectedState, playerId)
        }

        // Score each candidate
        val scored = candidates.map { action ->
            ScoredAction(action, evaluateAction(state, action, playerId))
        }

        // Pick the best action, but only if it's better than passing
        val best = scored.maxByOrNull { it.score }
        return if (best != null && best.score > passScore) {
            best.action
        } else {
            pass ?: legalActions.first()
        }
    }

    /**
     * Choose attackers during the declare attackers step.
     */
    fun chooseAttackers(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): LegalAction {
        // The combat enumerator provides a DeclareAttackers action
        // with validAttackers list. We need to decide which to attack with.
        return legalAction
    }

    private fun evaluateAction(state: GameState, action: LegalAction, playerId: EntityId): Double {
        val result = simulator.simulate(state, action.action)
        return evaluateResult(result, playerId)
    }

    private fun evaluateResult(result: SimulationResult, playerId: EntityId): Double {
        return evaluator.evaluate(result.state, result.state.projectedState, playerId)
    }

    /**
     * Remove obviously suboptimal actions to reduce the simulation space.
     */
    private fun prune(actions: List<LegalAction>, state: GameState, playerId: EntityId): List<LegalAction> {
        val opponentId = state.getOpponent(playerId)

        return actions.filter { action ->
            when (action.actionType) {
                // Always consider land plays
                "PlayLand" -> true

                // Always consider casting spells
                "CastSpell", "MorphCast" -> true

                // Consider activated abilities
                "ActivateAbility" -> true

                // Consider cycling
                "CycleCard", "TypecycleCard" -> true

                // Consider face-up
                "TurnFaceUp" -> true

                // Combat — always consider
                "DeclareAttackers", "DeclareBlockers" -> true

                // Crew — consider
                "CrewAbility" -> true

                // Graveyard abilities
                "GraveyardAbility" -> true

                // Unknown — include to be safe
                else -> true
            }
        }
    }
}

private data class ScoredAction(
    val action: LegalAction,
    val score: Double
)
