package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaRestriction.ColorlessSpellsOnly] (Sage of the Unknowable) and
 * [ManaRestriction.CostsContainingXOnly] (Rosheen, Roaring Prophet) against every payment shape.
 */
class ColorlessAndXCostManaRestrictionTest : FunSpec({

    val colorlessSpell = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT), isColorless = true)
    val coloredSpell = SpellPaymentContext(cardTypes = setOf(CardType.INSTANT), isInstantOrSorcery = true)
    val xSpell = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY), isInstantOrSorcery = true, hasXInCost = true)
    val xAbility = SpellPaymentContext(isAbilityActivation = true, hasXInCost = true)
    val plainAbility = SpellPaymentContext(isAbilityActivation = true)
    val faceUp = SpellPaymentContext(isTurnFaceUpAction = true)

    test("ColorlessSpellsOnly admits colorless spells, including face-down casts, and nothing else") {
        ManaRestriction.ColorlessSpellsOnly.isSatisfiedBy(colorlessSpell) shouldBe true
        ManaRestriction.ColorlessSpellsOnly.isSatisfiedBy(SpellPaymentContext.faceDownCast()) shouldBe true
        ManaRestriction.ColorlessSpellsOnly.isSatisfiedBy(coloredSpell) shouldBe false
        ManaRestriction.ColorlessSpellsOnly.isSatisfiedBy(plainAbility) shouldBe false
        ManaRestriction.ColorlessSpellsOnly.isSatisfiedBy(faceUp) shouldBe false
    }

    test("Sage's AnyOf admits colorless spells and every ability activation") {
        val sage = ManaRestriction.AnyOf(listOf(ManaRestriction.ColorlessSpellsOnly, ManaRestriction.AbilityActivationOnly))
        sage.isSatisfiedBy(colorlessSpell) shouldBe true
        sage.isSatisfiedBy(plainAbility) shouldBe true
        sage.isSatisfiedBy(coloredSpell) shouldBe false
    }

    test("CostsContainingXOnly admits spells and abilities whose cost has {X}") {
        ManaRestriction.CostsContainingXOnly.isSatisfiedBy(xSpell) shouldBe true
        ManaRestriction.CostsContainingXOnly.isSatisfiedBy(xAbility) shouldBe true
        ManaRestriction.CostsContainingXOnly.isSatisfiedBy(coloredSpell) shouldBe false
        ManaRestriction.CostsContainingXOnly.isSatisfiedBy(plainAbility) shouldBe false
        ManaRestriction.CostsContainingXOnly.isSatisfiedBy(faceUp) shouldBe false
    }

    test("an {X} ability doesn't satisfy the spell-only mana-value gate") {
        ManaRestriction.SpellsWithManaValueAtLeast(4, orXInCost = true).isSatisfiedBy(xAbility) shouldBe false
    }
})
