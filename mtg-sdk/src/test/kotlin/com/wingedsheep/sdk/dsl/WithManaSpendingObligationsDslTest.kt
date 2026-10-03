package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.effects.WithManaSpendingObligationsEffect
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WithManaSpendingObligationsDslTest : FunSpec({
    test("text replacement reaches the nested instruction") {
        val replacer = object : TextReplacer {
            override fun replaceCreatureType(subtype: String) = if (subtype == "Elf") "Goblin" else subtype
            override fun replaceSubtype(subtype: Subtype) = subtype
            override fun replaceColor(color: Color) = color
        }
        Effects.WithManaSpendingObligations(Effects.SetCreatureSubtypes(setOf("Elf"))).applyTextReplacement(replacer) shouldBe
            Effects.WithManaSpendingObligations(Effects.SetCreatureSubtypes(setOf("Goblin")))
    }
    test("transparent scope preserves resolved and unknown dynamic descriptions") {
        val nested = Effects.DrawCards(DynamicAmount.XValue)
        val scope = Effects.WithManaSpendingObligations(nested) as WithManaSpendingObligationsEffect
        scope.runtimeDescription { 3 } shouldBe "Draw 3 cards"
        scope.runtimeDescription { null } shouldBe nested.runtimeDescription { null }
    }
})
