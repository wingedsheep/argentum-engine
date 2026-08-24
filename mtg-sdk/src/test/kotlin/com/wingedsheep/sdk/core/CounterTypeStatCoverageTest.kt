package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CounterType.fromName` resolves a printed counter name back to its enum constant. Word-named
 * kinds go through `valueOf`; the stat kinds cannot, because `"+1/+0".uppercase()` is still
 * `"+1/+0"`, so they need the hand-written `STAT_COUNTERS` map.
 *
 * Hand-written is the problem this guards. `+2/+0` and `+0/+2` (Frankenstein's Monster) were added
 * to the enum and to [Counters] but never to that map, so `fromName("+2/+0")` answered null while
 * every other spelling of the same counter worked — silent, and reachable only by putting the one
 * card that uses it on a battlefield. Rather than re-list the kinds here and drift the same way,
 * this derives them from [Counters] by shape: a constant whose value looks like a stat modifier
 * must round-trip.
 */
class CounterTypeStatCoverageTest : DescribeSpec({

    // "+1/+1", "-2/-2", "+0/+2" — a sign, digits, a slash, a sign, digits.
    val statNamePattern = Regex("""^[+-]\d+/[+-]\d+$""")

    /** Every stat-shaped counter name [Counters] declares, read off the object reflectively. */
    val declaredStatNames: List<String> = Counters::class.java.declaredFields
        .filter { it.type == String::class.java }
        .mapNotNull {
            it.isAccessible = true
            it.get(Counters) as? String
        }
        .filter { statNamePattern.matches(it) }
        .distinct()

    describe("CounterType.fromName over the stat counters") {

        it("finds a plausible set of stat-named constants in Counters") {
            // Guards the reflection: a reshape of Counters must fail loudly here rather than
            // "passing" by checking an empty list.
            declaredStatNames.size shouldNotBe 0
            declaredStatNames.contains("+1/+1") shouldBe true
            declaredStatNames.contains("-1/-1") shouldBe true
        }

        it("resolves every stat counter name declared in Counters") {
            declaredStatNames.filter { CounterType.fromName(it) == null }.shouldBeEmpty()
        }

        it("resolves the asymmetric kinds that regressed") {
            CounterType.fromName("+2/+0") shouldBe CounterType.PLUS_TWO_PLUS_ZERO
            CounterType.fromName("+0/+2") shouldBe CounterType.PLUS_ZERO_PLUS_TWO
            CounterType.fromName("+1/+2") shouldBe CounterType.PLUS_ONE_PLUS_TWO
            CounterType.fromName("+2/+2") shouldBe CounterType.PLUS_TWO_PLUS_TWO
            CounterType.fromName("-2/-2") shouldBe CounterType.MINUS_TWO_MINUS_TWO
        }
    }
})
