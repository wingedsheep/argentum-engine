package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Chooses which [LegalAction] to take when the AI has priority.
 *
 * Uses 1-ply lookahead: simulate each affordable action, evaluate the
 * resulting board state, and pick the best. Combat decisions are delegated
 * to [CombatAdvisor] for MTG-specific attack/block heuristics.
 */
class Strategist(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val combatAdvisor: CombatAdvisor = CombatAdvisor(simulator, evaluator)
) {
    fun chooseAction(
        state: GameState,
        legalActions: List<LegalAction>,
        playerId: EntityId
    ): LegalAction {
        if (legalActions.size == 1) return legalActions.first()

        // Combat declaration steps are exclusive — only one action type available
        val combatAction = legalActions.find { it.actionType == "DeclareAttackers" || it.actionType == "DeclareBlockers" }
        if (combatAction != null && legalActions.all { it.actionType == combatAction.actionType }) {
            return handleCombatDeclaration(state, combatAction, playerId)
        }

        // Split into affordable non-mana actions vs pass/mana
        val pass = legalActions.find { it.actionType == "PassPriority" }
        val affordable = legalActions.filter { it.affordable && !it.isManaAbility && it.actionType != "PassPriority" }

        if (affordable.isEmpty()) return pass ?: legalActions.first()

        val candidates = prune(affordable, state, playerId)

        // Score the current state as baseline (what happens if we just pass)
        val passScore = if (pass != null) {
            evaluateAction(state, pass, playerId)
        } else {
            evaluator.evaluate(state, state.projectedState, playerId)
        }

        val scored = candidates.map { action ->
            ScoredAction(action, evaluateAction(state, action, playerId))
        }

        val best = scored.maxByOrNull { it.score }
        return if (best != null && best.score > passScore) {
            best.action
        } else {
            pass ?: legalActions.first()
        }
    }

    /**
     * Handle combat declaration steps by delegating to [CombatAdvisor].
     * Returns a new [LegalAction] with the combat advisor's chosen attackers/blockers.
     */
    private fun handleCombatDeclaration(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId
    ): LegalAction {
        val action = when (legalAction.actionType) {
            "DeclareAttackers" -> combatAdvisor.chooseAttackers(state, legalAction, playerId)
            "DeclareBlockers" -> combatAdvisor.chooseBlockers(state, legalAction, playerId)
            else -> legalAction.action
        }
        return legalAction.copy(action = action)
    }

    private fun evaluateAction(state: GameState, action: LegalAction, playerId: EntityId): Double {
        val result = simulator.simulate(state, action.action)
        return evaluator.evaluate(result.state, result.state.projectedState, playerId)
    }

    /**
     * Remove obviously suboptimal actions to reduce the simulation space.
     */
    private fun prune(actions: List<LegalAction>, state: GameState, playerId: EntityId): List<LegalAction> {
        return actions // For now, include everything — pruning heuristics can be added later
    }
}

private data class ScoredAction(
    val action: LegalAction,
    val score: Double
)
