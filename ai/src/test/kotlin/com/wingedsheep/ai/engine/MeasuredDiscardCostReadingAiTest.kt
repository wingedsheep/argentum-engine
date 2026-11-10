package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * Kozilek, the Great Distortion — "Discard a card with mana value X: Counter target spell with mana
 * value X." — and Gix, Yawgmoth Praetor's "Discard X cards" both pay with `AbilityCost.DiscardX`.
 *
 *  - A discard cost puts our own cards into our graveyard, so both feed the graveyard axis like a
 *    plain "Discard a card" cost does.
 *  - Kozilek's ability reads as a counterspell, whatever its cost shape.
 */
class MeasuredDiscardCostReadingAiTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }

    test("a measured or X-count discard cost feeds the graveyard axis") {
        SynergyAxes.of(card("Kozilek, the Great Distortion")).feeds shouldContain SynergyAxis.GRAVEYARD
        SynergyAxes.of(card("Gix, Yawgmoth Praetor")).feeds shouldContain SynergyAxis.GRAVEYARD
    }

    test("Kozilek's discard-for-a-counter ability reads as a counterspell") {
        CardIntentAnalyzer.analyze(card("Kozilek, the Great Distortion")).tags shouldContain IntentTag.COUNTERSPELL
    }
})
