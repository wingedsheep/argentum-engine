package com.wingedsheep.engine.core

/**
 * Engine-wide safety limits and saturating ("clamped") integer arithmetic.
 *
 * Magic has cards that drive *exponential or unbounded* growth in numbers — doubling tokens
 * (Doubling Season + a token maker), doubling counters, "twice the number of X" dynamic amounts,
 * and trigger/effect loops. Left unguarded, two things break:
 *
 *  1. **Memory explosion** — tokens are one ECS entity each, so a stack of doublers asks the
 *     engine to allocate 2^N entities one at a time → multi-second hang, then `OutOfMemoryError`.
 *  2. **Arithmetic overflow** — counters and [com.wingedsheep.sdk.scripting.values.DynamicAmount]
 *     are `Int`; doubling a billion-counter permanent silently wraps *negative*, corrupting state
 *     rather than crashing (worse — undetectable downstream).
 *
 * No real game ever needs the huge number: per the Magic Tournament Rules (loops are a shortcut,
 * a mandatory loop is a draw — see [StateBasedActionChecker]'s 104.4c handling), gigantic
 * quantities are never literally materialized. So the policy here is **clamp aggressively**: a
 * value that hits [MAX_QUANTITY] or an effect that hits a structural cap is, for every practical
 * purpose, "you've already won." We clamp (never wrap), and `System.err`-log when a cap actually
 * bites so a degenerate card/loop is debuggable after the fact — mirroring the SBA loop bail-out.
 *
 * These are *backstops*, not game rules: tuned far above any legitimate card so they never alter
 * real play, only catch the pathological tail.
 */
object GameLimits {

    /**
     * Upper (and lower) bound for any in-game quantity: counter counts, token counts, and
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount] results. 1e9 sits comfortably below
     * `Int.MAX_VALUE` (~2.1e9), so a couple more additions on top of a clamped value still can't
     * overflow, and it is astronomically larger than any real Magic quantity.
     */
    const val MAX_QUANTITY: Int = 1_000_000_000

    /**
     * Maximum number of token entities a single token-creation effect may materialize (after
     * count modifiers / doublers are applied). Each token is a full entity that every projection
     * and legal-action pass then scans, so this caps both the allocation burst and the resulting
     * per-step cost. Far above any real board; only a degenerate doubler stack reaches it.
     */
    const val MAX_TOKENS_PER_EFFECT: Int = 500

    /**
     * Maximum nesting/iteration depth of effect execution within a single resolution, enforced at
     * the [com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry] chokepoint via
     * [EffectContext.resolutionDepth][com.wingedsheep.engine.handlers.EffectContext.resolutionDepth].
     * Genuine card composition is only a handful deep; a self-perpetuating loop (e.g. a
     * `RepeatWhileEffect` whose condition never goes false) grows it without bound and would
     * otherwise `StackOverflowError`. Hitting the cap aborts that branch instead of crashing.
     *
     * Kept well below the JVM call-stack limit (each unit of depth is several stack frames) yet
     * far above any legitimate card — genuine composition is a handful deep, and the deepest real
     * `RepeatWhileEffect` loops are dozens of iterations at most.
     */
    const val MAX_RESOLUTION_DEPTH: Int = 500

    /**
     * Maximum body repetitions for a single `RepeatDynamicTimesEffect`. That executor materializes
     * one body per iteration before running them, so an unclamped dynamic count would OOM building
     * the list. Real "repeat N times" counts are tiny; this only bounds the pathological case.
     */
    const val MAX_REPEAT_ITERATIONS: Int = 10_000

    /**
     * Clamp a requested token-creation count to [MAX_TOKENS_PER_EFFECT], `System.err`-logging when
     * the cap actually bites so a runaway doubler combo is debuggable. Shared by every token
     * executor's allocation loop ([what] names the call site for the log line).
     */
    fun cappedTokenCount(requested: Int, what: String = "tokens"): Int {
        val capped = requested.coerceAtMost(MAX_TOKENS_PER_EFFECT)
        if (capped < requested) {
            System.err.println(
                "GameLimits: requested $requested $what exceeds $MAX_TOKENS_PER_EFFECT — clamping " +
                    "(likely a runaway token-doubling combo)."
            )
        }
        return capped
    }

    /** Clamp [value] into `[-MAX_QUANTITY, MAX_QUANTITY]`. */
    fun clamp(value: Int): Int = value.coerceIn(-MAX_QUANTITY, MAX_QUANTITY)

    /** Saturating `a + b`: computed in `Long` then clamped, so it can never overflow `Int`. */
    fun addClamped(a: Int, b: Int): Int = clampLong(a.toLong() + b.toLong())

    /** Saturating `a - b`. */
    fun subClamped(a: Int, b: Int): Int = clampLong(a.toLong() - b.toLong())

    /** Saturating `a * b`. */
    fun mulClamped(a: Int, b: Int): Int = clampLong(a.toLong() * b.toLong())

    private fun clampLong(value: Long): Int =
        value.coerceIn(-MAX_QUANTITY.toLong(), MAX_QUANTITY.toLong()).toInt()
}
