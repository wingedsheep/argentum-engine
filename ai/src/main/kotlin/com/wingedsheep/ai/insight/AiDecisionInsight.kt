package com.wingedsheep.ai.insight

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A record of one choice the engine AI actually made, with the preference it assigned to every
 * option it weighed.
 *
 * The AI already computes all of this — [com.wingedsheep.ai.engine.Strategist] scores each candidate
 * against passing, and [com.wingedsheep.ai.engine.CombatAdvisor] scores each attack/block plan
 * against not attacking — and then throws the numbers away and returns the winner. This is that
 * discarded workings, captured on the *real* decision path rather than re-derived: what the panel
 * shows is exactly what drove the move, not a second opinion computed for display.
 *
 * Recording is opt-in ([AiInsightSink] is null in production) because a sink also pins the
 * [GameState] each decision was made from, which is the half of a training example the scores alone
 * can't reconstruct.
 */
@Serializable
data class AiDecisionInsight(
    val kind: AiDecisionKind,
    /** The AI seat that made this choice. */
    val playerId: EntityId,
    val turnNumber: Int,
    /** [com.wingedsheep.sdk.core.Step] name, e.g. `PRECOMBAT_MAIN`. */
    val step: String,
    /** Null in the window before the first turn begins. */
    val activePlayerId: EntityId?,
    /** True when it was the AI's own turn. */
    val onOwnTurn: Boolean,
    /**
     * The "do nothing" reference every option is measured against — passing priority, or declaring
     * no attackers/blockers. An option only wins by beating it.
     */
    val baselineLabel: String,
    val baselineScore: Double,
    /** Label of the option in [options] the AI submitted. */
    val chosenLabel: String,
    val thinkTimeMs: Long,
    /** Scored options best-first, then the ones dropped before scoring. */
    val options: List<AiActionOption>,
)

@Serializable
enum class AiDecisionKind {
    /** A normal priority window: cast, activate, play a land, or pass. */
    PRIORITY,
    DECLARE_ATTACKERS,
    DECLARE_BLOCKERS,
}

/**
 * One option the AI weighed, and how much it wanted it.
 *
 * Scores are **raw board-evaluator units** — the same scale the AI compares internally. They are
 * only meaningful relative to each other and to [AiDecisionInsight.baselineScore], which is what
 * [advantage] expresses; there is no absolute "good score".
 */
@Serializable
data class AiActionOption(
    /** Human-readable, e.g. `Cast Lightning Bolt → Grizzly Bears`. */
    val label: String,
    /** [com.wingedsheep.engine.legalactions.LegalAction.actionType], e.g. `CastSpell`. */
    val actionType: String,
    val cardName: String? = null,
    /** Names of the targets the AI committed to, when it had a choice to make. */
    val targets: List<String> = emptyList(),
    /** Preference in evaluator units, or null when the option was dropped before it was scored. */
    val score: Double? = null,
    /**
     * The leaf score before per-card timing and advisor adjustment. Null when nothing adjusted it,
     * so a populated value is exactly the set of options where card knowledge changed the AI's mind.
     */
    val rawScore: Double? = null,
    /** [score] minus the baseline. Positive means the AI preferred this to doing nothing. */
    val advantage: Double? = null,
    val chosen: Boolean = false,
    /** True for the "pass priority" / "no attacks" row, so the UI can draw the waterline. */
    val baseline: Boolean = false,
    /** Why an option was dropped, or what adjusted its score. */
    val note: String? = null,
    /**
     * The exact action the AI would submit for this option — already materialized, already
     * simulated against the real processor.
     *
     * This is what makes an option *playable*. Held at a decision, the local testing mode can submit
     * this instead of the AI's own pick, so "what would have happened if it took the second-best
     * line?" is answered by the engine rather than argued about. Null only when the option cannot be
     * submitted (it failed materialization), which is also how the UI knows not to offer it.
     */
    val action: GameAction? = null,
)

/**
 * Where captured decisions go. Implemented by the host (the game server's local testing mode);
 * null everywhere else, which is what keeps recording off the production decision path.
 *
 * [state] is the position the decision was made from — the AI's own view of it, so a profile that
 * determinizes hidden information hands over the sampled world it actually searched.
 */
fun interface AiInsightSink {
    fun record(state: GameState, insight: AiDecisionInsight)
}
