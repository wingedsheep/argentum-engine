package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.wwk.cards.UrgeToFeed
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * +1/+1 counters put on each permanent of a gathered collection ("tap any number of untapped
 * Vampires you control … put a +1/+1 counter on each of those Vampires") feed the +1/+1-counter
 * axis the way a single-target counter does, so the deckbuilder sees Urge to Feed beside its
 * counter payoffs.
 */
class CollectionCountersSynergyAxesTest : FunSpec({

    test("counters on each permanent of a collection feed the +1/+1-counter axis") {
        SynergyAxes.of(UrgeToFeed).feeds shouldContain SynergyAxis.PLUS_COUNTERS
    }
})
