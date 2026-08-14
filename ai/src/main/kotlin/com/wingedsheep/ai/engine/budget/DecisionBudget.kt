package com.wingedsheep.ai.engine.budget

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * How much thinking a single decision is worth.
 *
 * The millisecond figures are the ones agreed in `backlog/engine-ai-improvement.md`. They are the
 * *nominal* size of a tier, not a stopwatch the search races: see [SearchAllowances] for why the
 * work a tier buys is counted, not timed.
 */
enum class BudgetTier(val millis: Long) {
    /** One legal action, or the window is auto-passable. The AI shouldn't be thinking at all. */
    TRIVIAL(0),

    /** An opponent's-turn window with no immediate threat; upkeep, draw, end step. */
    ROUTINE(200),

    /** Our main phase with meaningful actions. Most decisions. */
    NORMAL(2_000),

    /** Combat declaration; either player in lethal range; a sweeper or a real counterspell window. */
    CRITICAL(5_000);
}

/**
 * What a budget buys, expressed as **work**, not wall clock.
 *
 * A wall clock is the obvious way to spend a time budget and the wrong one here. The arena's whole
 * value rests on reruns at the same seed producing the same games — `ArenaHarnessTest` asserts
 * identical outcomes at 8 threads and at 1 — and a search that stops when the clock runs out
 * produces a different move under load than it does idle. So a tier is converted, once, into a
 * count of simulations the search may spend, and [DecisionBudget.deadlineNanos] is left as a hard
 * safety stop that a healthy decision never reaches.
 *
 * The numbers at [NORMAL_MILLIS] are exactly today's hardcoded constants, which is what makes
 * `AiProfile.LEGACY_V0` reproducible: `forMillis(2_000) == LEGACY`.
 */
data class SearchAllowances(
    /** Candidate targets simulated per target requirement. `Strategist.MAX_TARGET_CANDIDATES`. */
    val targetCandidates: Int,
    /** Whether committed targets are refined by simulation at all, or left at the heuristic pick. */
    val refineTargetsBySimulation: Boolean,
    /** Engine simulations the blocking local search may spend. `CombatAdvisor.MAX_BLOCK_SIMULATIONS`. */
    val blockSimulations: Int,
    /** Improvement rounds the attack local search may run. */
    val attackSearchIterations: Int,
    /** Wall-clock cap on the combat local searches specifically. Today's hardcoded 1000 ms. */
    val combatSearchMillis: Long,
    /**
     * Phase 7. Playouts one decision may spend across *all* its candidates, which
     * [com.wingedsheep.ai.engine.rollout.RolloutCandidateEvaluator] allocates by sequential
     * halving. Zero on every tier that gets the static evaluator instead.
     *
     * This is what keeps the rollout search reproducible. Spending a rollout budget as wall clock
     * would produce a different move under load than idle, which would make every arena rerun a
     * different game — the same argument that made the rest of this class a work count rather than
     * a stopwatch, applied to the one consumer that could actually spend an unbounded amount.
     */
    val rolloutPlayouts: Int = 0,
) {
    companion object {
        /** The tier the allowances are calibrated against. */
        val NORMAL_MILLIS: Long = BudgetTier.NORMAL.millis

        /**
         * Today's behaviour, constant for constant. `AiProfile.LEGACY_V0` must keep producing
         * exactly this or `FrozenBaselineTest` fails, which is the point of that test.
         */
        val LEGACY = SearchAllowances(
            targetCandidates = 8,
            refineTargetsBySimulation = true,
            blockSimulations = 10,
            attackSearchIterations = 3,
            combatSearchMillis = 1_000,
            // V0 has no rollouts, but `LegacyBudgetPolicy` is also what an agent that opts into
            // rollouts *without* opting into the tiered budget runs on — which is exactly the
            // isolation `v0-rollout` needs to be attributable. So the legacy allowance carries the
            // nominal count rather than zero, and the static evaluator simply never reads it.
            rolloutPlayouts = NORMAL_PLAYOUTS,
        )

        /**
         * Playouts a [BudgetTier.NORMAL] decision may spend.
         *
         * **Measured, not budgeted.** Phase 0's arithmetic says a 2 s tier *affords* ~60 playouts,
         * and the first cut spent them. Affording is not a reason to spend: the arena says strength
         * rises from 4 to 8 playouts and then flattens.
         *
         * | matchup | result |
         * |---|---|
         * | `v0` vs `v0-rollout-4` (300) | 53.7%, CI [49.3%, 57.3%] — spans parity |
         * | `v0` vs `v0-rollout-8` (300) | **57.3%, CI [53.0%, 61.7%]** |
         * | `v0-rollout-4` vs `v0-rollout-32` (400) | 50.7%, CI [47.5%, 53.7%] — flat |
         *
         * The plateau is not the usual "more samples always help a bit"; it says the rollout term
         * is **bias-limited rather than variance-limited**. It carries a quarter of the score
         * (`RolloutSettings.staticWeight`), common random numbers already pair its comparisons, and
         * what it cannot see — tempo — no amount of sampling reveals.
         *
         * So 16: comfortably above the 8 that demonstrated the win, well below the 32 that
         * demonstrably adds nothing, and 4× cheaper than the nominal budget. The headroom is
         * deliberate — the plateau was measured on one set at one `staticWeight`.
         */
        const val NORMAL_PLAYOUTS = 16

        /**
         * Scale [LEGACY] to a tier of [millis].
         *
         * Linear in the budget, then clamped: `blockSimulations` keeps 10 as a **floor** (the plan
         * is explicit that combat must never get *less* search than it has today), and every
         * allowance has a ceiling so a pathological budget can't turn one decision into a
         * multi-second stall. Below [BudgetTier.NORMAL] the simulation-refined target pick is
         * dropped entirely — it is the single most expensive thing a routine window pays for, at
         * up to `targetCandidates` simulations per requirement.
         */
        fun forMillis(millis: Long): SearchAllowances {
            if (millis <= 0) return TRIVIAL_ALLOWANCES
            val scale = millis.toDouble() / NORMAL_MILLIS
            return SearchAllowances(
                targetCandidates = scaled(LEGACY.targetCandidates, scale, floor = 1, ceiling = 32),
                refineTargetsBySimulation = millis >= NORMAL_MILLIS,
                blockSimulations = scaled(LEGACY.blockSimulations, scale, floor = 10, ceiling = 80),
                attackSearchIterations = scaled(LEGACY.attackSearchIterations, scale, floor = 1, ceiling = 12),
                combatSearchMillis = millis,
                // Below NORMAL the rollout evaluator falls back to the static one anyway (see
                // `RolloutCandidateEvaluator.scoreAll`), so the scaling only has to be sane from
                // NORMAL up. The ceiling keeps a pathological budget from turning one decision into
                // thousands of playouts.
                rolloutPlayouts = scaled(NORMAL_PLAYOUTS, scale, floor = 0, ceiling = 512),
            )
        }

        /** Nothing is searched; every consumer falls back to its heuristic seed. */
        val TRIVIAL_ALLOWANCES = SearchAllowances(
            targetCandidates = 1,
            refineTargetsBySimulation = false,
            blockSimulations = 0,
            attackSearchIterations = 0,
            combatSearchMillis = 0,
            rolloutPlayouts = 0,
        )

        private fun scaled(base: Int, scale: Double, floor: Int, ceiling: Int): Int =
            min(ceiling, max(floor, (base * scale).roundToInt()))
    }
}

/**
 * The time and work one decision may spend.
 *
 * Created per decision by a [BudgetPolicy] and threaded from `AIPlayer` down through `Strategist`,
 * `CombatAdvisor` and `DecisionResponder`. Every consumer must honour the **anytime contract**:
 * have a valid answer after its first iteration, so an expired budget degrades the move rather
 * than failing to produce one.
 */
class DecisionBudget(
    val tier: BudgetTier,
    val allowances: SearchAllowances,
    /** Nominal size of this budget in milliseconds. [UNBOUNDED_MILLIS] means "no wall-clock stop". */
    val millis: Long,
    private val startNanos: Long = System.nanoTime(),
) {
    /** Hard wall-clock stop. A healthy decision finishes long before this; see [SearchAllowances]. */
    val deadlineNanos: Long =
        if (millis >= UNBOUNDED_MILLIS) Long.MAX_VALUE else startNanos + millis * NANOS_PER_MILLI

    /** Deadline for the combat local searches, which have always had their own tighter cap. */
    val combatDeadlineNanos: Long =
        min(deadlineNanos, startNanos + allowances.combatSearchMillis * NANOS_PER_MILLI)

    fun expired(): Boolean = deadlineNanos != Long.MAX_VALUE && System.nanoTime() >= deadlineNanos

    fun remainingMs(): Long =
        if (deadlineNanos == Long.MAX_VALUE) Long.MAX_VALUE
        else max(0L, (deadlineNanos - System.nanoTime()) / NANOS_PER_MILLI)

    /** Milliseconds spent since this budget was opened. For latency reporting. */
    fun elapsedMs(): Long = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    override fun toString(): String = "DecisionBudget($tier, ${millis}ms, $allowances)"

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000L

        /** Sentinel for "the wall clock never stops this decision". */
        const val UNBOUNDED_MILLIS = Long.MAX_VALUE / NANOS_PER_MILLI

        /**
         * The budget `AiProfile.LEGACY_V0` runs on: today's constants, and no global deadline —
         * only the combat searches have ever had one, at 1000 ms.
         */
        fun legacy(): DecisionBudget =
            DecisionBudget(BudgetTier.NORMAL, SearchAllowances.LEGACY, UNBOUNDED_MILLIS)
    }
}
