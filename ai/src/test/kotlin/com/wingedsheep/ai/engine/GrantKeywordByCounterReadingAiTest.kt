package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.gtc.cards.CrownedCeratok
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "Each creature you control with a +1/+1 counter on it has trample" (Crowned Ceratok): the static
 * reads as the keyword grant it is, and as a payoff for the +1/+1-counter axis — with no counters
 * placed, it grants nothing.
 */
class GrantKeywordByCounterReadingAiTest : FunSpec({

    test("a keyword grant to our countered creatures reads as that keyword's grant") {
        CardIntentAnalyzer.analyze(CrownedCeratok).tags shouldContain IntentTag.EVASION_GRANT
    }

    test("a keyword grant to our countered creatures rewards the +1/+1 counters axis") {
        SynergyAxes.of(CrownedCeratok).rewards shouldContain SynergyAxis.PLUS_COUNTERS
    }
})
