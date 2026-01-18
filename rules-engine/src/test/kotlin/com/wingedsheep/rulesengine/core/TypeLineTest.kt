package com.wingedsheep.rulesengine.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

class TypeLineTest : FunSpec({

    context("parsing type lines") {
        test("parse simple creature") {
            val typeLine = TypeLine.parse("Creature — Dragon")
            typeLine.cardTypes shouldBe setOf(CardType.CREATURE)
            typeLine.subtypes shouldBe setOf(Subtype("Dragon"))
            typeLine.supertypes shouldBe emptySet()
        }

        test("parse creature with multiple subtypes") {
            val typeLine = TypeLine.parse("Creature — Human Soldier")
            typeLine.cardTypes shouldBe setOf(CardType.CREATURE)
            typeLine.subtypes shouldBe setOf(Subtype("Human"), Subtype("Soldier"))
        }

        test("parse legendary creature") {
            val typeLine = TypeLine.parse("Legendary Creature — Dragon")
            typeLine.supertypes shouldBe setOf(Supertype.LEGENDARY)
            typeLine.cardTypes shouldBe setOf(CardType.CREATURE)
            typeLine.subtypes shouldBe setOf(Subtype("Dragon"))
            typeLine.isLegendary.shouldBeTrue()
        }

        test("parse basic land") {
            val typeLine = TypeLine.parse("Basic Land — Plains")
            typeLine.supertypes shouldBe setOf(Supertype.BASIC)
            typeLine.cardTypes shouldBe setOf(CardType.LAND)
            typeLine.subtypes shouldBe setOf(Subtype("Plains"))
            typeLine.isBasicLand.shouldBeTrue()
        }

        test("parse sorcery") {
            val typeLine = TypeLine.parse("Sorcery")
            typeLine.cardTypes shouldBe setOf(CardType.SORCERY)
            typeLine.subtypes shouldBe emptySet()
        }

        test("parse artifact creature") {
            val typeLine = TypeLine.parse("Artifact Creature — Golem")
            typeLine.cardTypes shouldBe setOf(CardType.ARTIFACT, CardType.CREATURE)
            typeLine.subtypes shouldBe setOf(Subtype("Golem"))
        }

        test("parse with dash variant") {
            val typeLine = TypeLine.parse("Creature - Goblin")
            typeLine.cardTypes shouldBe setOf(CardType.CREATURE)
            typeLine.subtypes shouldBe setOf(Subtype("Goblin"))
        }
    }

    context("type checks") {
        test("isCreature") {
            TypeLine.parse("Creature — Dragon").isCreature.shouldBeTrue()
            TypeLine.parse("Sorcery").isCreature.shouldBeFalse()
        }

        test("isLand") {
            TypeLine.parse("Land").isLand.shouldBeTrue()
            TypeLine.parse("Basic Land — Forest").isLand.shouldBeTrue()
            TypeLine.parse("Creature — Elf").isLand.shouldBeFalse()
        }

        test("isSorcery") {
            TypeLine.parse("Sorcery").isSorcery.shouldBeTrue()
            TypeLine.parse("Instant").isSorcery.shouldBeFalse()
        }

        test("isPermanent") {
            TypeLine.parse("Creature — Goblin").isPermanent.shouldBeTrue()
            TypeLine.parse("Artifact").isPermanent.shouldBeTrue()
            TypeLine.parse("Enchantment").isPermanent.shouldBeTrue()
            TypeLine.parse("Land").isPermanent.shouldBeTrue()
            TypeLine.parse("Sorcery").isPermanent.shouldBeFalse()
            TypeLine.parse("Instant").isPermanent.shouldBeFalse()
        }
    }

    context("hasSubtype") {
        test("returns true for matching subtype") {
            val typeLine = TypeLine.parse("Creature — Human Wizard")
            typeLine.hasSubtype(Subtype("Human")).shouldBeTrue()
            typeLine.hasSubtype(Subtype("Wizard")).shouldBeTrue()
        }

        test("returns false for non-matching subtype") {
            val typeLine = TypeLine.parse("Creature — Goblin")
            typeLine.hasSubtype(Subtype("Elf")).shouldBeFalse()
        }
    }

    context("factory methods") {
        test("creature creates creature type line") {
            val typeLine = TypeLine.creature(setOf(Subtype.DRAGON))
            typeLine.isCreature.shouldBeTrue()
            typeLine.subtypes shouldBe setOf(Subtype.DRAGON)
        }

        test("sorcery creates sorcery type line") {
            val typeLine = TypeLine.sorcery()
            typeLine.isSorcery.shouldBeTrue()
        }

        test("basicLand creates basic land type line") {
            val typeLine = TypeLine.basicLand(Subtype.PLAINS)
            typeLine.isBasicLand.shouldBeTrue()
            typeLine.subtypes shouldBe setOf(Subtype.PLAINS)
        }
    }

    context("toString") {
        test("formats creature with subtypes") {
            val typeLine = TypeLine.parse("Creature — Dragon")
            typeLine.toString() shouldBe "Creature — Dragon"
        }

        test("formats legendary creature") {
            val typeLine = TypeLine.parse("Legendary Creature — Angel")
            typeLine.toString() shouldBe "Legendary Creature — Angel"
        }

        test("formats sorcery without subtypes") {
            val typeLine = TypeLine.sorcery()
            typeLine.toString() shouldBe "Sorcery"
        }
    }
})
