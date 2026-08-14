package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mechanic-level test for **copy exceptions** (CR 707.9b) — the shared `CopyExceptions` vocabulary
 * and the single [CopyExceptionApplier] every characteristic-carrying copy path runs through.
 *
 * The card-level behaviour lives in the per-card scenario tests (Shuri, Wakandan Inventor;
 * Absorbing Man; Taskmaster, Mercenary Mimic). This one pins the arithmetic those cards rely on,
 * plus the convergence itself: that the token effects' historical flat riders map onto the same
 * value type the permanent-copy path carries.
 */
class CopyExceptionsTest : FunSpec({

    fun legendaryArtifactBear() = CardComponent(
        cardDefinitionId = "Test Source#TST-1",
        name = "Test Source",
        manaCost = ManaCost.parse("{2}{G}"),
        typeLine = TypeLine(
            supertypes = setOf(Supertype.LEGENDARY),
            cardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE),
            subtypes = setOf(Subtype.BEAR),
        ),
        baseStats = CreatureStats(2, 2),
        baseKeywords = setOf(Keyword.TRAMPLE),
        colors = setOf(Color.GREEN),
    )

    fun plainArtifact() = CardComponent(
        cardDefinitionId = "Test Rock#TST-2",
        name = "Test Rock",
        manaCost = ManaCost.parse("{3}"),
        typeLine = TypeLine(cardTypes = setOf(CardType.ARTIFACT)),
    )

    test("no exceptions returns the copied component untouched") {
        val base = legendaryArtifactBear()
        CopyExceptionApplier.apply(base, CopyExceptions.None) shouldBe base
    }

    test("removedSupertypes strips the supertype — 'except it isn't legendary'") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
        )
        result.typeLine.isLegendary shouldBe false
        result.name shouldBe "Test Source"
    }

    test("a supertype named as both added and removed ends up removed") {
        val result = CopyExceptionApplier.apply(
            plainArtifact(),
            CopyExceptions(
                addedSupertypes = setOf(Supertype.LEGENDARY),
                removedSupertypes = setOf(Supertype.LEGENDARY),
            ),
        )
        result.typeLine.isLegendary shouldBe false
    }

    test("added types union onto the copied ones — 'in addition to its other types'") {
        val result = CopyExceptionApplier.apply(
            plainArtifact(),
            CopyExceptions(
                addedSupertypes = setOf(Supertype.LEGENDARY),
                addedCardTypes = setOf(CardType.CREATURE),
                addedSubtypes = setOf(Subtype.HUMAN, Subtype.VILLAIN),
            ),
        )
        result.typeLine.isArtifact shouldBe true
        result.typeLine.isCreature shouldBe true
        result.typeLine.isLegendary shouldBe true
        result.typeLine.subtypes shouldBe setOf(Subtype.HUMAN, Subtype.VILLAIN)
    }

    test("override types replace the copied ones — a stated type line with no 'in addition'") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(
                overrideCardTypes = setOf(CardType.CREATURE),
                overrideSubtypes = setOf(Subtype.HUMAN, Subtype.MERCENARY),
            ),
        )
        result.typeLine.isArtifact shouldBe false
        result.typeLine.isCreature shouldBe true
        result.typeLine.subtypes shouldBe setOf(Subtype.HUMAN, Subtype.MERCENARY)
    }

    test("an override wins over an addition on the same axis") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(
                addedCardTypes = setOf(CardType.ENCHANTMENT),
                overrideCardTypes = setOf(CardType.CREATURE),
                addedSubtypes = setOf(Subtype.HUMAN),
                overrideSubtypes = setOf(Subtype.GOBLIN),
                addedColors = setOf(Color.WHITE),
                overrideColors = setOf(Color.BLACK),
            ),
        )
        // Card types behave exactly like subtypes and colors: the replacing clause (CR 205.1a) wins
        // outright, so the addition is not layered back on top of it.
        result.typeLine.cardTypes shouldBe setOf(CardType.CREATURE)
        result.typeLine.subtypes shouldBe setOf(Subtype.GOBLIN)
        result.colors shouldBe setOf(Color.BLACK)
    }

    test("an addition on an axis with no override of its own still applies") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(
                addedCardTypes = setOf(CardType.ENCHANTMENT),
                overrideSubtypes = setOf(Subtype.GOBLIN),
            ),
        )
        result.typeLine.cardTypes shouldBe
            setOf(CardType.ARTIFACT, CardType.CREATURE, CardType.ENCHANTMENT)
        result.typeLine.subtypes shouldBe setOf(Subtype.GOBLIN)
    }

    test("added colors union onto the copied colors") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(addedColors = setOf(Color.RED)),
        )
        result.colors shouldBe setOf(Color.GREEN, Color.RED)
    }

    test("P/T overrides are conjured even when the copied object had none") {
        val result = CopyExceptionApplier.apply(
            plainArtifact(),
            CopyExceptions(powerOverride = 4, toughnessOverride = 4),
        )
        result.baseStats shouldNotBe null
        result.baseStats!!.power shouldBe CharacteristicValue.Fixed(4)
        result.baseStats!!.toughness shouldBe CharacteristicValue.Fixed(4)
    }

    test("a half-specified P/T override keeps the copied value on the other half") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(powerOverride = 7),
        )
        result.baseStats!!.power shouldBe CharacteristicValue.Fixed(7)
        result.baseStats!!.toughness shouldBe CharacteristicValue.Fixed(2)
    }

    test("added keywords are unioned into the copy's base keywords") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(addedKeywords = setOf(Keyword.VIGILANCE)),
        )
        result.baseKeywords shouldBe setOf(Keyword.TRAMPLE, Keyword.VIGILANCE)
    }

    test("nameOverride and noManaCost replace name and mana cost") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CopyExceptions(nameOverride = "Absorbing Man", noManaCost = true),
        )
        result.name shouldBe "Absorbing Man"
        result.manaCost shouldBe ManaCost.ZERO
        result.manaValue shouldBe 0
    }

    test("the token-copy effect's flat riders map onto the same CopyExceptions vocabulary") {
        val effect = CreateTokenCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0),
            overridePower = 5,
            overrideToughness = 5,
            addedKeywords = setOf(Keyword.FLYING),
            addedSupertypes = setOf(Supertype.LEGENDARY),
            removedSupertypes = setOf(Supertype.SNOW),
            overrideColors = setOf(Color.BLACK),
            overrideSubtypes = setOf(Subtype.DEMON),
            addCardTypes = setOf("ARTIFACT"),
            noManaCost = true,
        )
        effect.copyExceptions shouldBe CopyExceptions(
            addedKeywords = setOf(Keyword.FLYING),
            addedSupertypes = setOf(Supertype.LEGENDARY),
            removedSupertypes = setOf(Supertype.SNOW),
            addedCardTypes = setOf(CardType.ARTIFACT),
            overrideSubtypes = setOf(Subtype.DEMON),
            overrideColors = setOf(Color.BLACK),
            powerOverride = 5,
            toughnessOverride = 5,
            noManaCost = true,
        )
    }

    test("a token P/T override still needs both halves, as it always did") {
        val effect = CreateTokenCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0),
            overridePower = 5,
        )
        effect.copyExceptions.powerOverride shouldBe null
        effect.copyExceptions.toughnessOverride shouldBe null
    }

    test("the self-copy token effect's flat riders map onto the same vocabulary") {
        val effect = CreateTokenCopyOfSourceEffect(
            overridePower = 1,
            overrideToughness = 1,
            addCardTypes = setOf("ARTIFACT"),
            removeLegendary = true,
        )
        effect.copyExceptions shouldBe CopyExceptions(
            removedSupertypes = setOf(Supertype.LEGENDARY),
            addedCardTypes = setOf(CardType.ARTIFACT),
            powerOverride = 1,
            toughnessOverride = 1,
        )
    }

    test("an effect carrying only legacy riders is bit-for-bit unchanged by the merge") {
        // CopyExceptions.None.over(base) must be `base` itself, or every existing token-copy card
        // silently re-serializes.
        val riders = CreateTokenCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0),
            addedKeywords = setOf(Keyword.FLYING),
            overrideSubtypes = setOf(Subtype.DEMON),
        ).copyExceptions
        CopyExceptions.None.over(riders) shouldBe riders
    }

    test("the exceptions field and the legacy riders combine instead of dropping each other") {
        // The point of giving the token effects an `exceptions` field: a name override is not
        // expressible as a flat rider at all, and it must not cost the card its rider-borne types.
        val effect = CreateTokenCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0),
            addedKeywords = setOf(Keyword.FLYING),
            addCardTypes = setOf("ARTIFACT"),
            exceptions = CopyExceptions(
                nameOverride = "Mirror Image",
                addedKeywords = setOf(Keyword.VIGILANCE),
                powerOverride = 7,
            ),
        )
        effect.copyExceptions shouldBe CopyExceptions(
            nameOverride = "Mirror Image",
            // Additive axes union across the two sources.
            addedKeywords = setOf(Keyword.FLYING, Keyword.VIGILANCE),
            addedCardTypes = setOf(CardType.ARTIFACT),
            // A half-specified P/T is expressible on the field even though the riders demand both.
            powerOverride = 7,
        )
    }

    test("the modern field wins over a legacy rider on a replacing axis") {
        val effect = CreateTokenCopyOfTargetEffect(
            target = EffectTarget.ContextTarget(0),
            overrideSubtypes = setOf(Subtype.DEMON),
            exceptions = CopyExceptions(overrideSubtypes = setOf(Subtype.GOBLIN)),
        )
        effect.copyExceptions.overrideSubtypes shouldBe setOf(Subtype.GOBLIN)
    }

    test("the self-copy effect takes an exceptions field too") {
        val effect = CreateTokenCopyOfSourceEffect(
            removeLegendary = true,
            exceptions = CopyExceptions(nameOverride = "Absorbing Man"),
        )
        effect.copyExceptions shouldBe CopyExceptions(
            nameOverride = "Absorbing Man",
            removedSupertypes = setOf(Supertype.LEGENDARY),
        )
    }

    test("a stated type line renders with an article, a bare supertype clause without one") {
        // Taskmaster's replacing clause, Absorbing Man's additive one, and Shuri's removal.
        CopyExceptions(
            addedSupertypes = setOf(Supertype.LEGENDARY),
            overrideCardTypes = setOf(CardType.CREATURE),
            overrideSubtypes = setOf(Subtype.HUMAN),
        ).clauses() shouldBe listOf("it's a legendary Human creature")

        CopyExceptions(
            addedCardTypes = setOf(CardType.ARTIFACT),
        ).clauses() shouldBe listOf("it's an artifact in addition to its other types")

        CopyExceptions(
            addedSupertypes = setOf(Supertype.LEGENDARY),
        ).clauses() shouldBe listOf("it's legendary")

        CopyExceptions(
            removedSupertypes = setOf(Supertype.LEGENDARY),
        ).clauses() shouldBe listOf("it isn't legendary")
    }

    test("the self-copy 'except it isn't legendary' clause strips the supertype through the applier") {
        val result = CopyExceptionApplier.apply(
            legendaryArtifactBear(),
            CreateTokenCopyOfSourceEffect(removeLegendary = true).copyExceptions,
        )
        result.typeLine.isLegendary shouldBe false
        result.typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)
    }
})
