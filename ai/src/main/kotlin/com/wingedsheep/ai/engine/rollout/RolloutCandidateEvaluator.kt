package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import kotlin.math.max

/**
 * Scores a candidate by the mean of several short playouts instead of one static evaluation.
 *
 * This is the primary strength lever of `backlog/engine-ai-improvement.md` — the one phase whose
 * job is to move a win rate rather than enable one. Everything above it is unchanged: candidate
 * generation, target selection, the hold policy and the `CardAdvisor` override path all still see
 * a raw evaluator score, and a per-card advisor now overrides a much better base.
 *
 * ### Why the four variance-reduction tricks are not optional
 *
 * A rollout mean is a noisy estimate, and with the handful of samples a 2 s budget buys, raw
 * sampling noise is comfortably larger than the difference between two reasonable candidates. All
 * four of these exist to make the *comparison* precise rather than the estimate:
 *
 * 1. **Common random numbers.** Playout *(d, r)* of every candidate runs on the same seed, so the
 *    same shuffles and the same policy coin-flips happen in each. Candidate comparisons become
 *    paired and between-candidate noise mostly cancels; without it you need roughly 4× the
 *    playouts for the same discrimination. [seedFor] is the grid.
 * 2. **Shared determinizations.** The `d` axis of the same grid. Pinned at one world until Phase 8
 *    lands a `Determinizer` — see [RolloutSettings.determinizations] for why it exists now anyway.
 * 3. **Early cutoff** on a decided position, behind [RolloutSettings.earlyCutoffMargin] because it
 *    trades a small bias for the speedup and deserves its own A/B.
 * 4. **Sequential halving.** Below.
 *
 * ### Sequential halving is also how the budget gets spent
 *
 * Give every candidate one playout, drop the worse half, double the per-candidate allocation,
 * repeat. Contenders get 2–4× the samples they would under an even split, and the mechanism is
 * **inherently anytime**: after the first round every candidate has a score, so an exhausted budget
 * degrades the move rather than failing to produce one.
 *
 * The pass is one of the candidates, deliberately. If passing is eliminated in an early round its
 * score stays low and the best action beats it — which is the correct reading of "every line we
 * looked at beat doing nothing".
 *
 * ### Reproducibility
 *
 * Nothing here reads a clock. Every seed derives from the root state and every allocation from
 * `SearchAllowances.rolloutPlayouts`, so a rerun at the same arena seed plays the same game — the
 * property `ArenaHarnessTest` asserts by playing at 8 threads and at 1. `DecisionBudget.expired()`
 * is consulted only as the hard safety stop it was designed to be.
 */
class RolloutCandidateEvaluator(
    private val playouts: Playouts,
    /** The leaf inside a playout, and the fallback for tiers that get no playouts at all. */
    private val staticEvaluator: BoardEvaluator,
    private val settings: RolloutSettings = RolloutSettings.DEFAULT,
    private val winProbabilityScale: Double = WinProbability.SCALE,
) : CandidateEvaluator {

    /**
     * A single candidate, scored on its own.
     *
     * There is no halving to do with one candidate, so this spends the whole per-candidate
     * allocation on it. The Strategist always goes through [scoreAll]; this exists for the
     * interface's sake and for callers holding exactly one state.
     */
    override fun score(
        root: GameState,
        afterAction: GameState,
        playerId: EntityId,
        budget: DecisionBudget,
    ): Double = scoreAll(root, listOf(afterAction), playerId, budget).first()

    override fun scoreAll(
        root: GameState,
        afterActions: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<Double> {
        if (afterActions.isEmpty()) return emptyList()

        // TRIVIAL and ROUTINE get the static evaluator, per the plan's tier table. A routine
        // opponent's-turn window is not worth a playout, and spending one there is how a budget
        // ladder stops being monotone.
        val playoutBudget = budget.allowances.rolloutPlayouts
        if (playoutBudget <= 0 || budget.tier <= BudgetTier.ROUTINE) {
            return afterActions.map { staticEvaluator.evaluate(it, it.projectedState, playerId) }
        }

        val horizon = settings.horizonPlayerTurns +
            if (budget.tier == BudgetTier.CRITICAL) settings.criticalHorizonBonus else 0
        val rootSeed = rootSeedFor(root, playerId)

        // The reference every playout measures against. Shared across candidates on purpose: it is
        // what cancels the evaluator's uncalibrated absolute scale (see `PlayoutEngine.leafValue`)
        // while leaving candidate-to-candidate differences intact. Taken **raw** — squashing it
        // first would clamp away exactly the offset it exists to subtract — with the terminal
        // sentinel guarded, since `Double.MAX_VALUE / 2` is a marker rather than a board score.
        val baseline = WinProbability.asBaseline(
            staticEvaluator.evaluate(root, root.projectedState, playerId)
        )

        val samples = List(afterActions.size) { RunningMean() }
        // The (d, r) grid coordinate each candidate has consumed so far. Sequential halving gives
        // survivors *more* playouts rather than repeating the ones they already ran, and every
        // candidate walks the same grid in the same order — that is what makes the comparison
        // paired.
        val consumed = IntArray(afterActions.size)

        var survivors = afterActions.indices.toList()
        val rounds = max(1, ceilLog2(afterActions.size))
        var spent = 0

        while (survivors.isNotEmpty()) {
            val remaining = playoutBudget - spent
            if (remaining <= 0) break
            // The standard allocation: split what is left evenly across this round's survivors,
            // and never allocate zero — a round that runs no playouts cannot rank anything.
            val perCandidate = max(1, remaining / (rounds * survivors.size))

            for (index in survivors) {
                repeat(perCandidate) {
                    val grid = consumed[index]++
                    val determinization = grid % settings.determinizations
                    val rollout = grid / settings.determinizations
                    val value = playouts.run(
                        afterActions[index],
                        playerId,
                        seedFor(rootSeed, determinization, rollout),
                        horizon,
                        baseline,
                    )
                    samples[index].add(value)
                    spent++
                }
                // The safety stop, not the schedule. A healthy decision never reaches it; when it
                // does, every candidate already has a score from round one.
                if (budget.expired()) break
            }

            if (survivors.size <= 1 || !settings.sequentialHalving || budget.expired()) break
            survivors = survivors
                .sortedByDescending { samples[it].mean() }
                .take(max(1, survivors.size / 2))
        }

        // Back to raw evaluator units, with the baseline restored so the number stays on the scale
        // every consumer above the leaf is written in — the pass comparison, the hold policy's
        // timing delta, and a `CardAdvisor` returning `defaultScore + 2.0`. A candidate whose
        // playouts land where the root did scores exactly the root's value; a candidate that wins
        // every playout scores `baseline + logit(1.0)`, which dominates finitely.
        //
        // Both terms are expressed as a *bounded delta from the same baseline*, which is what makes
        // them mixable: the static leaf goes through the same squash, so a terminal leaf's
        // `Double.MAX_VALUE / 2` sentinel becomes ±55 instead of poisoning the average, and the
        // arbitrary offset that `ThreatAssessment` bakes into every absolute score cancels once for
        // both. See [RolloutSettings.staticWeight] for why the mixture exists at all.
        val w = settings.staticWeight
        return afterActions.indices.map { index ->
            val rolloutDelta = WinProbability.logit(samples[index].mean(), winProbabilityScale)
            if (w <= 0.0) return@map baseline + rolloutDelta
            val staticLeaf = staticEvaluator.evaluate(
                afterActions[index], afterActions[index].projectedState, playerId
            )
            val staticDelta = WinProbability.logit(
                WinProbability.squash(staticLeaf - baseline, winProbabilityScale),
                winProbabilityScale,
            )
            baseline + w * staticDelta + (1.0 - w) * rolloutDelta
        }
    }

    /**
     * The seed grid every candidate shares.
     *
     * SplitMix64's finalizing mix over the three coordinates, so `(d, r)` cells are far apart in the
     * stream and adding a candidate cannot shift another candidate's worlds.
     */
    private fun seedFor(rootSeed: Long, determinization: Int, rollout: Int): Long =
        mix(rootSeed xor (determinization.toLong() * GRID_D) xor (rollout.toLong() * GRID_R))

    /**
     * A seed for this decision, as a pure function of the position.
     *
     * Pure is the requirement, not the choice of ingredients: a counter, a clock or a hash code of a
     * mutable object would all make two runs of the same arena game diverge. `GameState.rng` alone
     * would do on its own, except that it only advances when the game actually draws randomness, so
     * a quiet stretch of turns would reuse one grid for every decision in it — the step and stack
     * terms break that up.
     */
    private fun rootSeedFor(root: GameState, playerId: EntityId): Long = mix(
        root.rng.state
            xor (root.turnNumber.toLong() * TURN_SALT)
            xor (root.step.ordinal.toLong() * STEP_SALT)
            xor (root.stack.size.toLong() * STACK_SALT)
            xor playerId.value.hashCode().toLong()
    )

    /** Streaming mean — the estimate has to be readable between rounds to rank survivors. */
    private class RunningMean {
        private var total = 0.0
        private var count = 0

        fun add(value: Double) {
            total += value
            count++
        }

        /** [WinProbability.DRAW] for a candidate that never ran, which is the honest prior. */
        fun mean(): Double = if (count == 0) WinProbability.DRAW else total / count
    }

    private companion object {
        const val GRID_D = 0x1_0000_0001L
        const val GRID_R = 0x9E37_79B9L
        const val TURN_SALT = 0x2545_F491L
        const val STEP_SALT = 0x1405_7B7EL
        const val STACK_SALT = 0x5851_F42DL

        /** SplitMix64's finalizing mix. Same one `GameRng` uses; scrambles a Weyl value hard. */
        fun mix(z0: Long): Long {
            var z = z0 + -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }

        fun ceilLog2(n: Int): Int {
            var rounds = 0
            var remaining = n
            while (remaining > 1) {
                remaining = (remaining + 1) / 2
                rounds++
            }
            return rounds
        }
    }
}
