package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.advisor.CardAdvisorRegistry
import com.wingedsheep.engine.ai.advisor.CastContext
import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Chooses which [LegalAction] to take when the AI has priority.
 *
 * Two-phase evaluation:
 * 1. **Quick pass**: Score all candidates with 1-ply lookahead.
 * 2. **Deep pass**: If the top candidates are close, re-score them with
 *    multi-ply [Searcher] to break the tie considering opponent responses.
 *
 * Combat decisions are delegated to [CombatAdvisor].
 * Card-specific overrides are handled by [CardAdvisorRegistry].
 */
class Strategist(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val searcher: Searcher = Searcher(simulator, evaluator),
    private val combatAdvisor: CombatAdvisor = CombatAdvisor(simulator, evaluator),
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry()
) {
    fun chooseAction(
        state: GameState,
        legalActions: List<LegalAction>,
        playerId: EntityId
    ): LegalAction {
        // Combat declaration steps need the CombatAdvisor to fill in attacker/blocker maps
        // even when there's only one legal action (which is the common case — the enumerator
        // returns a single DeclareAttackers/DeclareBlockers with an empty default map).
        val combatAction = legalActions.find { it.actionType == "DeclareAttackers" || it.actionType == "DeclareBlockers" }
        if (combatAction != null) {
            return handleCombatDeclaration(state, combatAction, playerId)
        }

        if (legalActions.size == 1) return legalActions.first()

        val pass = legalActions.find { it.actionType == "PassPriority" }
        val affordable = legalActions.filter { it.affordable && !it.isManaAbility && it.actionType != "PassPriority" }

        if (affordable.isEmpty()) return pass ?: legalActions.first()

        // ── Phase 1: Quick 1-ply scoring of all candidates ──
        val passScore = if (pass != null) {
            evaluate1Ply(state, pass, playerId)
        } else {
            evaluator.evaluate(state, state.projectedState, playerId)
        }

        val scored = affordable.map { action ->
            action to evaluate1Ply(state, action, playerId, passScore)
        }

        // ── Phase 2: Adaptive deep search on close contenders ──
        val depth = searcher.recommendDepth(state, scored, playerId)

        val finalScored = if (depth > 1) {
            deepSearch(state, scored, playerId, depth, passScore)
        } else {
            scored
        }

        val best = finalScored.maxByOrNull { it.second }
        return if (best != null && best.second > passScore) {
            best.first
        } else {
            pass ?: legalActions.first()
        }
    }

    /**
     * Re-score top candidates with multi-ply search.
     * Only the top N contenders get deep search — the rest keep their 1-ply scores.
     */
    private fun deepSearch(
        state: GameState,
        scored: List<Pair<LegalAction, Double>>,
        playerId: EntityId,
        depth: Int,
        passScore: Double
    ): List<Pair<LegalAction, Double>> {
        val sorted = scored.sortedByDescending { it.second }
        val bestScore = sorted.first().second

        // Only deep-search actions within striking distance of the best
        val threshold = (bestScore - passScore).coerceAtLeast(1.0) * 0.5
        val contenders = sorted.takeWhile { it.second >= bestScore - threshold }
            .take(6) // hard cap
        val contenderSet = contenders.map { it.first }.toSet()

        return scored.map { (action, quickScore) ->
            if (action in contenderSet) {
                action to searcher.searchAction(state, action, playerId, depth)
            } else {
                action to quickScore
            }
        }
    }

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

    private fun evaluate1Ply(state: GameState, action: LegalAction, playerId: EntityId, passScore: Double? = null): Double {
        val result = simulator.simulate(state, action.action)
        val defaultScore = evaluator.evaluate(result.state, result.state.projectedState, playerId)

        // Check for card-specific advisor override
        val cardName = resolveCardName(state, action) ?: return defaultScore
        val advisor = advisorRegistry.getAdvisor(cardName) ?: return defaultScore
        val context = CastContext(
            state = state,
            projected = state.projectedState,
            playerId = playerId,
            action = action,
            passScore = passScore ?: evaluator.evaluate(state, state.projectedState, playerId),
            defaultScore = defaultScore,
            evaluator = evaluator,
            simulator = simulator
        )
        return advisor.evaluateCast(context) ?: defaultScore
    }

    /** Resolve the card name from a legal action's underlying GameAction. */
    private fun resolveCardName(state: GameState, action: LegalAction): String? {
        val entityId = when (val gameAction = action.action) {
            is CastSpell -> gameAction.cardId
            is ActivateAbility -> gameAction.sourceId
            else -> return null
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.name
    }
}
