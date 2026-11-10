package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [ManaRestriction.ColorlessSpellsOrAbilitiesOnly] (Sage of the Unknowable, Cultivator Drone) and
 * [ManaRestriction.CostsContainingSymbolOnly] (Rosheen, Roaring Prophet's {X}; Cultivator Drone's
 * {C}) against every payment shape.
 */
class ColorlessAndXCostManaRestrictionTest : FunSpec({

    val colorlessSpell = SpellPaymentContext(cardTypes = setOf(CardType.ARTIFACT), isColorless = true)
    val coloredSpell = SpellPaymentContext(cardTypes = setOf(CardType.INSTANT), isInstantOrSorcery = true)
    val xSpell = SpellPaymentContext(
        cardTypes = setOf(CardType.SORCERY), isInstantOrSorcery = true, hasXInCost = true,
        costSymbols = ManaCost.parse("{X}{R}").symbols.toSet(),
    )
    val xAbility = SpellPaymentContext(
        isAbilityActivation = true, hasXInCost = true, costSymbols = ManaCost.parse("{X}").symbols.toSet(),
    )
    val plainAbility = SpellPaymentContext(isAbilityActivation = true)
    val faceUp = SpellPaymentContext(isTurnFaceUpAction = true)
    val colorlessPermanentAbility = SpellPaymentContext(
        isAbilityActivation = true, isAbilitySourceColorless = true, isAbilitySourceOnBattlefield = true,
    )
    val colorlessCardAbilityInHand = SpellPaymentContext(isAbilityActivation = true, isAbilitySourceColorless = true)
    val coloredPermanentAbility = SpellPaymentContext(isAbilityActivation = true, isAbilitySourceOnBattlefield = true)
    val cSpell = SpellPaymentContext(
        cardTypes = setOf(CardType.CREATURE), isCreature = true,
        costSymbols = ManaCost.parse("{2}{U}{C}").symbols.toSet(),
    )
    val cAbilityOfColoredPermanent = coloredPermanentAbility.copy(costSymbols = ManaCost.parse("{1}{C}").symbols.toSet())

    test("the default admits colorless spells, including face-down casts, and nothing else") {
        val colorless = ManaRestriction.ColorlessSpellsOrAbilitiesOnly()
        colorless.isSatisfiedBy(colorlessSpell) shouldBe true
        colorless.isSatisfiedBy(SpellPaymentContext.faceDownCast()) shouldBe true
        colorless.isSatisfiedBy(coloredSpell) shouldBe false
        colorless.isSatisfiedBy(plainAbility) shouldBe false
        colorless.isSatisfiedBy(colorlessPermanentAbility) shouldBe false
        colorless.isSatisfiedBy(faceUp) shouldBe false
    }

    test("allowAbilities admits abilities of colorless sources only; permanentSourcesOnly narrows to the battlefield") {
        val anySource = ManaRestriction.ColorlessSpellsOrAbilitiesOnly(allowSpells = false, allowAbilities = true)
        anySource.isSatisfiedBy(colorlessPermanentAbility) shouldBe true
        anySource.isSatisfiedBy(colorlessCardAbilityInHand) shouldBe true
        anySource.isSatisfiedBy(coloredPermanentAbility) shouldBe false
        anySource.isSatisfiedBy(colorlessSpell) shouldBe false

        val permanents = anySource.copy(permanentSourcesOnly = true)
        permanents.isSatisfiedBy(colorlessPermanentAbility) shouldBe true
        permanents.isSatisfiedBy(colorlessCardAbilityInHand) shouldBe false
    }

    test("Sage's AnyOf admits colorless spells and every ability activation") {
        val sage = ManaRestriction.AnyOf(listOf(ManaRestriction.ColorlessSpellsOrAbilitiesOnly(), ManaRestriction.AbilityActivationOnly))
        sage.isSatisfiedBy(colorlessSpell) shouldBe true
        sage.isSatisfiedBy(plainAbility) shouldBe true
        sage.isSatisfiedBy(coloredSpell) shouldBe false
    }

    test("CostsContainingSymbolOnly(X) admits spells and abilities whose cost has {X}") {
        val x = ManaRestriction.CostsContainingSymbolOnly(ManaSymbol.X)
        x.isSatisfiedBy(xSpell) shouldBe true
        x.isSatisfiedBy(xAbility) shouldBe true
        x.isSatisfiedBy(coloredSpell) shouldBe false
        x.isSatisfiedBy(plainAbility) shouldBe false
        x.isSatisfiedBy(faceUp) shouldBe false
        x.isSatisfiedBy(cSpell) shouldBe false
    }

    test("CostsContainingSymbolOnly(C) admits any cost with {C}: a colored spell, a colored permanent's ability, a resolution cost") {
        val c = ManaRestriction.CostsContainingSymbolOnly(ManaSymbol.Colorless)
        c.isSatisfiedBy(cSpell) shouldBe true
        c.isSatisfiedBy(cAbilityOfColoredPermanent) shouldBe true
        c.isSatisfiedBy(SpellPaymentContext.forResolutionCost(ManaCost.parse("{1}{C}"))) shouldBe true
        c.isSatisfiedBy(SpellPaymentContext.forResolutionCost(ManaCost.parse("{2}"))) shouldBe false
        c.isSatisfiedBy(xSpell) shouldBe false
        c.isSatisfiedBy(faceUp) shouldBe false
    }

    test("Cultivator Drone's composite admits each of its three clauses and nothing else") {
        val drone = ManaRestriction.AnyOf(listOf(
            ManaRestriction.ColorlessSpellsOrAbilitiesOnly(allowAbilities = true, permanentSourcesOnly = true),
            ManaRestriction.CostsContainingSymbolOnly(ManaSymbol.Colorless),
        ))
        drone.isSatisfiedBy(colorlessSpell) shouldBe true
        drone.isSatisfiedBy(colorlessPermanentAbility) shouldBe true
        drone.isSatisfiedBy(cSpell) shouldBe true
        drone.isSatisfiedBy(cAbilityOfColoredPermanent) shouldBe true
        drone.isSatisfiedBy(coloredSpell) shouldBe false
        drone.isSatisfiedBy(coloredPermanentAbility) shouldBe false
        drone.isSatisfiedBy(colorlessCardAbilityInHand) shouldBe false
        drone.isSatisfiedBy(SpellPaymentContext.forResolutionCost(ManaCost.parse("{1}"))) shouldBe false
        drone.description shouldBe "Spend this mana only to cast colorless spells or activate abilities of " +
            "colorless permanents, or pay costs that contain {C}"
    }

    test("a resolution cost is neither a cast nor an activation: only negative restrictions and AnySpend admit it") {
        val resolution = SpellPaymentContext.forResolutionCost(ManaCost.parse("{1}"))
        ManaRestriction.AnySpend.isSatisfiedBy(resolution) shouldBe true
        ManaRestriction.CannotCastSpellsFromHand.isSatisfiedBy(resolution) shouldBe true
        ManaRestriction.SpellsOnly.isSatisfiedBy(resolution) shouldBe false
        ManaRestriction.AbilityActivationOnly.isSatisfiedBy(resolution) shouldBe false
        ManaRestriction.ColorlessSpellsOrAbilitiesOnly(allowAbilities = true).isSatisfiedBy(resolution) shouldBe false
    }

    test("an {X} ability doesn't satisfy the spell-only mana-value gate") {
        ManaRestriction.SpellsWithManaValueAtLeast(4, orXInCost = true).isSatisfiedBy(xAbility) shouldBe false
    }
})
