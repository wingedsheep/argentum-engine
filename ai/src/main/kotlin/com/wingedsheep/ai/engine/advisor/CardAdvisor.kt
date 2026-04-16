package com.wingedsheep.engine.ai.advisor

import com.wingedsheep.engine.ai.GameSimulator
import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Per-card (or per-pattern) AI advisor that overrides generic AI behavior
 * for specific cards where domain-specific strategy matters.
 *
 * Advisors are **opt-in** — cards without an advisor use the existing
 * simulation-based scoring. Each method returns null to defer to generic logic.
 *
 * A single advisor can handle multiple cards (e.g., one "board wipe advisor"
 * for Wrath of God, Day of Judgment, etc.).
 */
interface CardAdvisor {
    /**
     * Card name(s) this advisor handles.
     * Matched against [CardComponent.name] of the spell being cast
     * or the source of a pending decision.
     */
    val cardNames: Set<String>

    /**
     * Adjust the score for casting this spell or activated ability.
     *
     * Called during [Strategist]'s Phase 1 scoring after the default 1-ply
     * simulation has already been computed (available as [CastContext.defaultScore]).
     *
     * Return non-null to **replace** the simulation score.
     * Return null to keep the default score.
     */
    fun evaluateCast(context: CastContext): Double? = null

    /**
     * Override a decision made during this card's resolution.
     *
     * Called before [DecisionResponder]'s generic logic. Receives the full
     * decision and game state — can simulate outcomes via [AdvisorDecisionContext.simulator].
     *
     * Return non-null to **replace** the generic response.
     * Return null to fall through to default behavior.
     */
    fun respondToDecision(context: AdvisorDecisionContext): DecisionResponse? = null

    /**
     * Score penalty for attacking with this creature.
     *
     * Called by [CombatAdvisor] for each valid attacker whose card name
     * matches this advisor. The penalty is subtracted from the attack
     * evaluation score, making it harder for the creature to pass the
     * aggression threshold.
     *
     * Return a positive value to disincentivize attacking (e.g., 10.0
     * makes it very unlikely to attack, but a lethal alpha strike can
     * still override). Return 0.0 or null to use the default combat logic.
     */
    fun attackPenalty(state: GameState, projected: ProjectedState, entityId: EntityId, playerId: EntityId): Double? = null
}

/**
 * Context available when evaluating whether to cast a spell.
 */
data class CastContext(
    val state: GameState,
    val projected: ProjectedState,
    val playerId: EntityId,
    val action: LegalAction,
    /** Score of passing priority (doing nothing). */
    val passScore: Double,
    /** Score from the default 1-ply simulation. */
    val defaultScore: Double,
    val evaluator: BoardEvaluator,
    val simulator: GameSimulator
)

/**
 * Context available when responding to a decision during card resolution.
 *
 * Named [AdvisorDecisionContext] to avoid collision with [com.wingedsheep.engine.core.DecisionContext].
 */
data class AdvisorDecisionContext(
    val state: GameState,
    val projected: ProjectedState,
    val playerId: EntityId,
    val decision: PendingDecision,
    /** Name of the card whose resolution triggered this decision. */
    val sourceCardName: String,
    val evaluator: BoardEvaluator,
    val simulator: GameSimulator
)
