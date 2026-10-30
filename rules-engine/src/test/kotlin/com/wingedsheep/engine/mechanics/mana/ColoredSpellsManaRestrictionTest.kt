package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaRestriction.ColoredSpellsOnly] (Titans' Nest: "a spell that's one or more colors without
 * {X} in its mana cost") against every payment shape.
 */
class ColoredSpellsManaRestrictionTest : FunSpec({

    val monoColored = SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), isCreature = true, colorCount = 1)
    val multicolored = SpellPaymentContext(cardTypes = setOf(CardType.INSTANT), isInstantOrSorcery = true, colorCount = 3)
    val coloredX = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY), isInstantOrSorcery = true, colorCount = 1, hasXInCost = true)
    val colorless = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT), isColorless = true, colorCount = 0)
    val unknownColors = SpellPaymentContext(cardTypes = setOf(CardType.CREATURE), isCreature = true)
    val coloredAbility = SpellPaymentContext(isAbilityActivation = true, colorCount = 1)
    val faceUp = SpellPaymentContext(isTurnFaceUpAction = true)

    test("admits spells with one or more colors, X or not, and nothing else") {
        val any = ManaRestriction.ColoredSpellsOnly()
        any.isSatisfiedBy(monoColored) shouldBe true
        any.isSatisfiedBy(multicolored) shouldBe true
        any.isSatisfiedBy(coloredX) shouldBe true
        any.isSatisfiedBy(colorless) shouldBe false
        any.isSatisfiedBy(SpellPaymentContext.faceDownCast()) shouldBe false
        any.isSatisfiedBy(coloredAbility) shouldBe false
        any.isSatisfiedBy(faceUp) shouldBe false
    }

    test("withoutXInCost refuses a colored spell whose mana cost has {X}") {
        val nest = ManaRestriction.ColoredSpellsOnly(withoutXInCost = true)
        nest.isSatisfiedBy(monoColored) shouldBe true
        nest.isSatisfiedBy(multicolored) shouldBe true
        nest.isSatisfiedBy(coloredX) shouldBe false
        nest.isSatisfiedBy(colorless) shouldBe false
    }

    test("a context that doesn't set colorCount refuses the mana") {
        ManaRestriction.ColoredSpellsOnly().isSatisfiedBy(unknownColors) shouldBe false
    }
})
