package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.WithManaAbilitySourcesEffect
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ManaAbilitySourcesDslTest : FunSpec({
    val replacer = object : TextReplacer {
        override fun replaceCreatureType(subtype: String): String = if (subtype == "Elf") "Goblin" else subtype
        override fun replaceSubtype(subtype: Subtype): Subtype = subtype
        override fun replaceColor(color: Color): Color = if (color == Color.GREEN) Color.BLUE else color
    }

    test("text replacement reaches both the mana-source filter and the nested instruction") {
        val scope = Effects.WithManaAbilitySources(
            Effects.SetCreatureSubtypes(setOf("Elf")), GameObjectFilter.Land.withColor(Color.GREEN))
        scope.applyTextReplacement(replacer) shouldBe Effects.WithManaAbilitySources(
            Effects.SetCreatureSubtypes(setOf("Goblin")), GameObjectFilter.Land.withColor(Color.BLUE))
    }

    test("a transparent scope preserves resolved and unknown dynamic descriptions") {
        val nested = Effects.DrawCards(DynamicAmount.XValue)
        val scope = Effects.WithManaAbilitySources(nested, GameObjectFilter.Land) as WithManaAbilitySourcesEffect
        scope.runtimeDescription { 3 } shouldBe "Draw 3 cards"
        scope.runtimeDescription { null } shouldBe nested.runtimeDescription { null }
    }
})
