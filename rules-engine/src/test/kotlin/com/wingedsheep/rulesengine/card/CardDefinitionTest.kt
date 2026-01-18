package com.wingedsheep.rulesengine.card

import com.wingedsheep.rulesengine.core.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.assertions.throwables.shouldThrow

class CardDefinitionTest : FunSpec({

    context("creature factory") {
        test("creates a creature with all properties") {
            val card = CardDefinition.creature(
                name = "Alabaster Dragon",
                manaCost = ManaCost.parse("{4}{W}{W}"),
                subtypes = setOf(Subtype.DRAGON),
                power = 4,
                toughness = 4,
                keywords = setOf(Keyword.FLYING)
            )

            card.name shouldBe "Alabaster Dragon"
            card.cmc shouldBe 6
            card.colors shouldBe setOf(Color.WHITE)
            card.isCreature.shouldBeTrue()
            card.isPermanent.shouldBeTrue()
            card.creatureStats shouldBe CreatureStats(4, 4)
            card.hasKeyword(Keyword.FLYING).shouldBeTrue()
        }

        test("creates vanilla creature without keywords") {
            val card = CardDefinition.creature(
                name = "Grizzly Bears",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype.BEAST),
                power = 2,
                toughness = 2
            )

            card.keywords shouldBe emptySet()
            card.hasKeyword(Keyword.FLYING).shouldBeFalse()
        }
    }

    context("sorcery factory") {
        test("creates a sorcery") {
            val card = CardDefinition.sorcery(
                name = "Lava Axe",
                manaCost = ManaCost.parse("{4}{R}"),
                oracleText = "Lava Axe deals 5 damage to target player or planeswalker."
            )

            card.name shouldBe "Lava Axe"
            card.cmc shouldBe 5
            card.colors shouldBe setOf(Color.RED)
            card.isSorcery.shouldBeTrue()
            card.isPermanent.shouldBeFalse()
            card.creatureStats shouldBe null
        }
    }

    context("basicLand factory") {
        test("creates a basic land") {
            val card = CardDefinition.basicLand("Plains", Subtype.PLAINS)

            card.name shouldBe "Plains"
            card.cmc shouldBe 0
            card.colors shouldBe emptySet()
            card.isLand.shouldBeTrue()
            card.isPermanent.shouldBeTrue()
            card.typeLine.isBasicLand.shouldBeTrue()
        }
    }

    context("validation") {
        test("creature must have stats") {
            shouldThrow<IllegalArgumentException> {
                CardDefinition(
                    name = "Invalid Creature",
                    manaCost = ManaCost.parse("{1}{G}"),
                    typeLine = TypeLine.creature(),
                    creatureStats = null
                )
            }
        }

        test("sorcery can omit stats") {
            val card = CardDefinition(
                name = "Lightning Bolt",
                manaCost = ManaCost.parse("{R}"),
                typeLine = TypeLine.sorcery(),
                oracleText = "Deal 3 damage to any target."
            )
            card.creatureStats shouldBe null
        }
    }

    context("color identity") {
        test("single color") {
            val card = CardDefinition.creature(
                name = "Test",
                manaCost = ManaCost.parse("{2}{W}"),
                subtypes = emptySet(),
                power = 1,
                toughness = 1
            )
            card.colorIdentity shouldBe setOf(Color.WHITE)
        }

        test("multicolor") {
            val card = CardDefinition.creature(
                name = "Test",
                manaCost = ManaCost.parse("{W}{U}{B}"),
                subtypes = emptySet(),
                power = 1,
                toughness = 1
            )
            card.colorIdentity shouldBe setOf(Color.WHITE, Color.BLUE, Color.BLACK)
        }

        test("colorless") {
            val card = CardDefinition.creature(
                name = "Test",
                manaCost = ManaCost.parse("{4}"),
                subtypes = emptySet(),
                power = 2,
                toughness = 2
            )
            card.colorIdentity shouldBe emptySet()
        }
    }

    context("type checks") {
        test("isCreature") {
            CardDefinition.creature("Test", ManaCost.ZERO, emptySet(), 1, 1).isCreature.shouldBeTrue()
            CardDefinition.sorcery("Test", ManaCost.ZERO, "").isCreature.shouldBeFalse()
        }

        test("isLand") {
            CardDefinition.basicLand("Forest", Subtype.FOREST).isLand.shouldBeTrue()
            CardDefinition.creature("Test", ManaCost.ZERO, emptySet(), 1, 1).isLand.shouldBeFalse()
        }

        test("isSorcery") {
            CardDefinition.sorcery("Test", ManaCost.ZERO, "").isSorcery.shouldBeTrue()
            CardDefinition.creature("Test", ManaCost.ZERO, emptySet(), 1, 1).isSorcery.shouldBeFalse()
        }
    }
})
