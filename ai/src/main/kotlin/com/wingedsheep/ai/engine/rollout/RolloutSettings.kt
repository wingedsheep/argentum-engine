package com.wingedsheep.ai.engine.rollout

/**
 * How a [RolloutCandidateEvaluator] spends its budget.
 *
 * Lives on `AiProfile`, so a rollout configuration is a named arena competitor rather than a set of
 * constants: `just arena v0-rollout v0-rollout-deep 1000` is two agents differing in nothing but
 * this object.
 *
 * *How many* playouts a decision gets is deliberately **not** here — that comes from
 * `SearchAllowances.rolloutPlayouts`, derived from the decision's budget tier, because the whole
 * point of Phase 4b is that a critical decision searches harder than a routine one. This carries the
 * shape of a playout, not its quantity.
 */
data class RolloutSettings(
    /**
     * How far a playout runs, in **player turns** from the root.
     *
     * 2 is the plan's "end of the opponent's next turn" at a duel: our turn finishes, theirs
     * happens, and the playout stops as their turn hands back — so the crack-back is inside the
     * horizon and its consequences are on the board being evaluated. `GameState.turnNumber` counts
     * player turns, so at a pod this is a shorter wall-clock horizon than at a duel, which is the
     * cheap direction to be wrong in.
     */
    val horizonPlayerTurns: Int = 2,

    /**
     * Extra player turns a [com.wingedsheep.ai.engine.budget.BudgetTier.CRITICAL] decision looks
     * ahead. Lethal ranges and combat declarations are where the extra turn pays.
     */
    val criticalHorizonBonus: Int = 1,

    /**
     * Hard cap on engine actions in one playout. A backstop, not a schedule: a healthy two-turn
     * playout is ~40–60 actions post-auto-pass, and this only binds when a board locks into a loop
     * the horizon can't end.
     */
    val maxActionsPerPlayout: Int = 150,

    /**
     * Softmax temperature for the playout policy's action choice.
     *
     * **Stochasticity is not optional.** A deterministic policy makes every playout of a candidate
     * identical, which collapses R rollouts into one sample and turns the whole mechanism into a
     * slower static evaluator with extra steps. τ≈1 over a score range of ~0–2 is a policy that
     * usually plays the best-looking thing and sometimes doesn't.
     */
    val temperature: Double = 1.0,

    /**
     * Allocate the playout budget by sequential halving rather than spreading it evenly.
     *
     * Off is the honest control, not a fallback: `just arena v0-rollout-flat v0-rollout 1000`
     * measures what the allocation is worth on its own.
     */
    val sequentialHalving: Boolean = true,

    /**
     * Stop a playout early once the squashed leaf score is this far from even.
     *
     * A real speedup with a real bias — a position at 0.97 occasionally loses — so the plan is
     * explicit that it ships behind a flag and gets A/B'd. Null disables it, which is the default.
     */
    val earlyCutoffMargin: Double? = null,

    /**
     * How much of a candidate's score comes from the static leaf rather than the playout mean.
     *
     * **A pure rollout (0.0) measures worse than the greedy AI it replaces, and the reason is
     * structural rather than statistical.** Passing in your own main phase does not end the turn —
     * it advances a step — and the playout policy then casts the very spell you just declined, a
     * window or a phase later. Two turns downstream the "cast it now" line and the "pass" line have
     * converged to the same board, so the rollout mean cannot see the tempo difference between
     * them, and the Strategist's strict `>` sends the tie to passing. Measured on the puzzle suite:
     * removal 6/6 → 2/6, sequencing 5/6 → 3/6 and non-creature valuation 2/6 → 0/6, every failure
     * an unnecessary pass — 48/66 against `v0`'s 55/66.
     *
     * The two estimators are blind to different things. The static leaf sees exactly the tempo the
     * rollout misses — "the board right after this resolves" — and the rollout sees exactly what
     * one ply cannot: prevention effects, the crack-back, whether a race is actually won. Mixing
     * them is the standard remedy and it recovers both.
     *
     * At 1.0 this is `AiProfile.LEGACY_V0`'s leaf exactly, which makes the parameter its own
     * control: `just arena v0-rollout-pure v0-rollout 1000` prices the mixture directly.
     *
     * 0.75 is where the 48-puzzle suite peaked (34 / 39 / 39 / **40**, against `v0`'s 39) and where
     * the arena number was measured. Phase 2b's expanded 66-puzzle suite does **not** reproduce the
     * peak — 0.25, 0.5 and 0.75 are indistinguishable at 55/66 — so this is now an unvalidated
     * choice inside a plateau rather than a fitted one. What both suites agree on is that mixing
     * *some* static leaf back in is worth ~7 puzzles over mixing none. Phase 9's logistic fit is
     * where a guess like this stops being a guess.
     */
    val staticWeight: Double = 0.75,

    /**
     * Determinizations per decision (Phase 8's K).
     *
     * Present and pinned at 1 because the seed grid is `(determinization, rollout)` and building
     * that grid one dimension at a time later would mean rewriting the common-random-number scheme
     * that variance reduction rests on. Until Phase 8 lands a `Determinizer`, all K worlds are the
     * same world, so anything above 1 buys nothing and the constructor rejects it.
     */
    val determinizations: Int = 1,
) {
    init {
        require(horizonPlayerTurns >= 1) { "A playout must cover at least one player turn." }
        require(maxActionsPerPlayout >= 1) { "maxActionsPerPlayout must be positive." }
        require(temperature > 0.0) { "Softmax temperature must be positive." }
        require(staticWeight in 0.0..1.0) { "staticWeight is a mixing fraction, was $staticWeight." }
        require(determinizations == 1) {
            "K > 1 needs Phase 8's Determinizer; until then every world is the same world."
        }
    }

    companion object {
        /** The configuration the `v0-rollout` arena agent runs. */
        val DEFAULT = RolloutSettings()
    }
}
