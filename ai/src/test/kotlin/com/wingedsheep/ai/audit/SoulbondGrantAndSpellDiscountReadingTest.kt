package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * A soulbond pair's granted activated ability reads as if printed on the pair (Stonewright's pump,
 * Stern Mentor's mill, Galvanic Alchemist's self-untap), and a discount on every caster's instants
 * and sorceries (Arcane Melee) reads as a cost reducer that rewards the spells axis.
 */
class SoulbondGrantAndSpellDiscountReadingTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }

    test("a soulbond pair's granted pump reads as the pump it grants") {
        CardIntentAnalyzer.analyze(card("Stonewright")).tags shouldContain IntentTag.PUMP
    }

    test("a soulbond pair's granted mill reads as mill") {
        CardIntentAnalyzer.analyze(card("Stern Mentor")).tags shouldContain IntentTag.MILL
    }

    test("a soulbond pair's granted self-untap reads as a combat enhancement") {
        CardIntentAnalyzer.analyze(card("Galvanic Alchemist")).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
    }

    test("a discount on every caster's instants and sorceries is a cost reducer rewarding spells") {
        val melee = card("Arcane Melee")
        CardIntentAnalyzer.analyze(melee).tags shouldContain IntentTag.COST_REDUCER
        SynergyAxes.of(melee).rewards shouldContain SynergyAxis.SPELLS
    }
})
