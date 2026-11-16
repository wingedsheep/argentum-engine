package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.dtk.cards.AvatarOfTheResolute
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "Enters with a +1/+1 counter on it for each other creature you control with a +1/+1 counter on
 * it" (Avatar of the Resolute) grows with the counters the deck has already put down, so it pays
 * off the +1/+1-counter axis as well as feeding it.
 */
class PlusCounterCountEntrySynergyAxesTest : FunSpec({

    test("an entry count of our creatures with +1/+1 counters rewards the counters axis") {
        val profile = SynergyAxes.of(AvatarOfTheResolute)
        profile.rewards shouldContain SynergyAxis.PLUS_COUNTERS
        profile.feeds shouldContain SynergyAxis.PLUS_COUNTERS
    }
})
