package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Sacrifice a land" (Hammer of Purphoros, Zuran Orb) eats a permanent that never dies, so the
 * cost is no sacrifice outlet and feeds no death trigger — the deckbuilder must not pair it with
 * "whenever a creature you control dies" payoffs. "Sacrifice a creature" (Phyrexian Ghoul) stays one.
 */
class LandSacrificeCostReadingTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("a land-only sacrifice cost is not a sacrifice outlet") {
        CardIntentAnalyzer.analyze(card("Hammer of Purphoros")).tags shouldNotContain IntentTag.SACRIFICE_OUTLET
        CardIntentAnalyzer.analyze(card("Zuran Orb")).tags shouldNotContain IntentTag.SACRIFICE_OUTLET
        withClue("a creature sacrifice cost stays an outlet") {
            CardIntentAnalyzer.analyze(card("Phyrexian Ghoul")).tags shouldContain IntentTag.SACRIFICE_OUTLET
        }
    }

    test("a land-only sacrifice cost feeds no death axis") {
        SynergyAxes.of(card("Zuran Orb")).feeds shouldNotContain SynergyAxis.DEATH
        withClue("a creature sacrifice cost still feeds it") {
            SynergyAxes.of(card("Phyrexian Ghoul")).feeds shouldContain SynergyAxis.DEATH
        }
    }
})
