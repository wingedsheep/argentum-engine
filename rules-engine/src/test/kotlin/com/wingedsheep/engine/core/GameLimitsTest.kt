package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [GameLimits] — the saturating arithmetic and structural caps that keep
 * number/token "explosion" cards (doubling counters/tokens, exponential dynamic amounts) from
 * overflowing `Int` or exhausting memory. See `backlog/number-explosion-safety.md`.
 */
class GameLimitsTest : FunSpec({

    test("addClamped saturates instead of overflowing Int") {
        // Plain `Int.MAX_VALUE + Int.MAX_VALUE` wraps to -2; addClamped must saturate instead.
        GameLimits.addClamped(Int.MAX_VALUE, Int.MAX_VALUE) shouldBe GameLimits.MAX_QUANTITY
        GameLimits.addClamped(GameLimits.MAX_QUANTITY, GameLimits.MAX_QUANTITY) shouldBe GameLimits.MAX_QUANTITY
        GameLimits.addClamped(3, 4) shouldBe 7
    }

    test("subClamped saturates at the negative bound") {
        GameLimits.subClamped(-GameLimits.MAX_QUANTITY, GameLimits.MAX_QUANTITY) shouldBe -GameLimits.MAX_QUANTITY
        GameLimits.subClamped(Int.MIN_VALUE, Int.MAX_VALUE) shouldBe -GameLimits.MAX_QUANTITY
        GameLimits.subClamped(10, 3) shouldBe 7
    }

    test("mulClamped saturates instead of overflowing Int") {
        // 1.5e9 * 2 overflows a signed Int to a negative value without the guard.
        GameLimits.mulClamped(1_500_000_000, 2) shouldBe GameLimits.MAX_QUANTITY
        GameLimits.mulClamped(GameLimits.MAX_QUANTITY, 1_000) shouldBe GameLimits.MAX_QUANTITY
        GameLimits.mulClamped(-1_500_000_000, 2) shouldBe -GameLimits.MAX_QUANTITY
        GameLimits.mulClamped(6, 7) shouldBe 42
    }

    test("clamp bounds both directions") {
        GameLimits.clamp(Int.MAX_VALUE) shouldBe GameLimits.MAX_QUANTITY
        GameLimits.clamp(Int.MIN_VALUE) shouldBe -GameLimits.MAX_QUANTITY
        GameLimits.clamp(0) shouldBe 0
    }

    test("cappedTokenCount clamps above the per-effect cap and passes small counts through") {
        GameLimits.cappedTokenCount(50_000) shouldBe GameLimits.MAX_TOKENS_PER_EFFECT
        GameLimits.cappedTokenCount(GameLimits.MAX_TOKENS_PER_EFFECT + 1) shouldBe GameLimits.MAX_TOKENS_PER_EFFECT
        GameLimits.cappedTokenCount(3) shouldBe 3
        GameLimits.cappedTokenCount(0) shouldBe 0
    }

    test("CountersComponent.withAdded clamps instead of wrapping to a negative count") {
        // A near-2-billion counter count, doubled, would overflow Int to negative without the
        // saturating add inside withAdded.
        val huge = 1_500_000_000
        val doubled = CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to huge))
            .withAdded(CounterType.PLUS_ONE_PLUS_ONE, huge)
            .getCount(CounterType.PLUS_ONE_PLUS_ONE)

        doubled shouldBe GameLimits.MAX_QUANTITY
        doubled shouldBeGreaterThan 0 // the bug being guarded against: wrapping negative
    }
})
