package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.math.exp
import kotlin.math.ln

/**
 * The bridge between the evaluator's raw score space and the probability space rollouts average in.
 *
 * A rollout produces *outcomes*, and outcomes have to be averaged as probabilities. Averaging raw
 * board scores does not work here for a concrete reason:
 * [com.wingedsheep.ai.engine.evaluation.CompositeBoardEvaluator] returns `Double.MAX_VALUE / 2` for
 * a won game, so the mean of "one win and three even boards" overflows to a number that beats every
 * other candidate no matter what the other three said. In probability space the same four samples
 * are `(1.0 + 0.5 + 0.5 + 0.5) / 4 = 0.625`, which is what "one line in four wins outright" actually
 * means.
 *
 * Everything *outside* the averaging stays in raw score space. The Strategist's per-card timing
 * deltas, the `CardAdvisor` overrides and the pass comparison are all written in evaluator units and
 * compose additively there, so [RolloutCandidateEvaluator] averages in probability space and hands
 * back a [logit] — the raw-space score whose win probability is the one it measured. One conversion
 * at each end, and no consumer above the leaf has to learn a new scale.
 *
 * [SCALE] is the one free parameter, and it is a hand-set guess like every other constant in
 * `BoardFeatures.kt`. Phase 9's logistic fit produces it for free: it is exactly `1/w` for the
 * fitted `P(win) = σ(w · x)`, so the fit that replaces the evaluation weights replaces this too.
 */
object WinProbability {

    /**
     * Raw evaluator points per logit.
     *
     * Calibrated by eye against the evaluator's own units: board presence carries weight 1.5 and a
     * creature is worth roughly 2–4 points, so a two-creature advantage is ~9 raw points. At
     * [SCALE] = 6 that reads as 0.82 — "clearly ahead, not yet won", which is about right. Phase 9
     * fits it.
     */
    const val SCALE: Double = 6.0

    /**
     * How far from certainty a squashed score may land.
     *
     * [logit] of an exact 1.0 is infinity, and an infinite candidate score poisons every average and
     * comparison downstream. Clamping to 1e-4 puts a proven win at ±55 raw points — far above any
     * board score the features can produce (a full board plus a 20-life lead is ~30), so a win still
     * dominates everything, but it dominates *finitely* and stays averageable.
     */
    private const val EPSILON: Double = 1e-4

    /** A terminal win. */
    const val WIN: Double = 1.0

    /** A terminal loss, or elimination from a pod (CR 104.3b) while the game carries on. */
    const val LOSS: Double = 0.0

    /** A draw, and the neutral prior for a position nothing is known about. */
    const val DRAW: Double = 0.5

    /** Logistic squash of a raw evaluator score into a win probability. */
    fun squash(score: Double, scale: Double = SCALE): Double {
        require(scale.isFinite() && scale > 0.0) { "Win-probability scale must be positive and finite" }
        if (score.isNaN()) return DRAW
        // The guard matters: the static evaluator's terminal sentinel is `Double.MAX_VALUE / 2`,
        // and `exp(-MAX_VALUE/12)` is a denormal-flush to 0 on the way to 1.0 anyway — but the
        // explicit branch documents the intent and skips the arithmetic.
        if (score >= TERMINAL_RAW) return WIN - EPSILON
        if (score <= -TERMINAL_RAW) return LOSS + EPSILON
        return 1.0 / (1.0 + exp(-score / scale))
    }

    /**
     * Inverse of [squash]: the raw evaluator score whose win probability is [p].
     *
     * Clamped to [EPSILON] either side, so a proven win comes back as a large finite number rather
     * than an infinity that would break every comparison it touches.
     */
    fun logit(p: Double, scale: Double = SCALE): Double {
        require(scale.isFinite() && scale > 0.0) { "Win-probability scale must be positive and finite" }
        if (p.isNaN()) return 0.0
        val clamped = p.coerceIn(EPSILON, 1.0 - EPSILON)
        return scale * ln(clamped / (1.0 - clamped))
    }

    /**
     * The win probability of a state the playout stopped at because it *ended*, or null when the
     * game is still live and the leaf evaluator has to answer instead.
     *
     * Two different questions, and a pod makes them different: `gameOver` asks whether the table is
     * finished, and [GameState.teamActivePlayers] asks whether *we* are (CR 104.3b — a player loses
     * and the game carries on). Without the second, a playout that eliminates us scores off the
     * surviving opponents' boards, which can read as better than surviving.
     */
    fun terminalValue(state: GameState, playerId: EntityId): Double? = when {
        state.gameOver -> when {
            state.winnerId == null -> DRAW
            // `winnerId` names one representative of the winning team (GameEndCheck), so a Two-
            // Headed Giant teammate who is not that representative has still won.
            state.winnerId in state.teamOf(playerId) -> WIN
            else -> LOSS
        }
        state.teamActivePlayers(playerId).isEmpty() -> LOSS
        else -> null
    }

    /**
     * [score] as a subtractable reference point, or 0.0 when it is not a real board evaluation.
     *
     * The raw evaluator has no calibrated zero — `ThreatAssessment` prices "we can never kill them"
     * with a 99-turn sentinel, so an ordinary position where one side has no creatures scores
     * around −176 while a close board is single digits. Subtracting the decision's root before
     * squashing is what removes that offset (see `PlayoutEngine.leafValue`), and it only works on
     * the **raw** number: routing the baseline through [squash] first would clamp away precisely
     * the magnitude it exists to cancel.
     *
     * The guard is for the terminal sentinel, which is a marker rather than a score.
     */
    fun asBaseline(score: Double): Double =
        if (!score.isFinite() || score >= TERMINAL_RAW || score <= -TERMINAL_RAW) 0.0 else score

    /** Above this, a raw score is a terminal sentinel rather than a board evaluation. */
    private const val TERMINAL_RAW: Double = 1e9
}
