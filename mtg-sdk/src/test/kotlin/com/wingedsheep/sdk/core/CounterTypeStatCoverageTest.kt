package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * The stat counters (CR 122.1a — `+1/+1`, `-1/-0`, `+0/+2`) are the kinds whose printed spelling is
 * not a word, so neither direction of the id ↔ printed mapping can fall out of `lowercase()`. Both
 * are derived from the id's shape rather than listed by hand, because a hand-written list is how
 * `+2/+0` and `+0/+2` (Frankenstein's Monster) once came to be declared and unreachable by name.
 * This pins that every stat-shaped kind the SDK knows survives the round trip.
 */
class CounterTypeStatCoverageTest : DescribeSpec({

    // "+1/+1", "-2/-2", "+0/+2" — a sign, a digit, a slash, a sign, a digit.
    val statSpelling = Regex("""^[+-]\d/[+-]\d$""")
    val statKinds = CounterType.KNOWN.filter { statSpelling.matches(it.printed) }

    describe("the stat counters") {

        it("finds all twelve by their printed shape") {
            statKinds.size shouldBe 12
            statKinds shouldContainAll listOf(CounterType.PLUS_ONE_PLUS_ONE, CounterType.MINUS_TWO_MINUS_TWO)
        }

        it("reads every stat kind back from its printed spelling") {
            statKinds.filter { CounterType.of(it.printed) != it }.shouldBeEmpty()
        }

        it("spells the asymmetric kinds that once regressed") {
            CounterType.of("+2/+0") shouldBe CounterType.PLUS_TWO_PLUS_ZERO
            CounterType.of("+0/+2") shouldBe CounterType.PLUS_ZERO_PLUS_TWO
            CounterType.of("-1/-0") shouldBe CounterType.MINUS_ONE_MINUS_ZERO
            CounterType.MINUS_ZERO_MINUS_ONE.printed shouldBe "-0/-1"
            CounterType.of("-0/-2") shouldBe CounterType.MINUS_ZERO_MINUS_TWO
        }

        it("gives every stat kind the delta its printed spelling names") {
            statKinds.filter { kind ->
                val (p, t) = kind.printed.split('/').map { it.toInt() }
                kind.ptDelta != (p to t)
            }.shouldBeEmpty()
            CounterType.MINUS_ZERO_MINUS_TWO.ptDelta shouldBe (0 to -2)
            CounterType.PLUS_ONE_PLUS_TWO.ptDelta shouldBe (1 to 2)
        }

        it("gives non-stat kinds no delta") {
            CounterType.CHARGE.ptDelta shouldBe null
            CounterType.FIRST_STRIKE.ptDelta shouldBe null
            CounterType.of("study").ptDelta shouldBe null
        }
    }
})
