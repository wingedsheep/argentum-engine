package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CommanderEligibilityTest : FunSpec({

    test("legendary creature is a legal commander") {
        val card = CardDefinition.creature(
            name = "Yuriko, the Tiger's Shadow",
            manaCost = ManaCost.parse("{1}{U}{B}"),
            subtypes = setOf(Subtype("Human"), Subtype("Ninja")),
            power = 1,
            toughness = 3,
            supertypes = setOf(Supertype.LEGENDARY),
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("non-legendary creature is not a legal commander by default") {
        val card = CardDefinition.creature(
            name = "Grizzly Bears",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        )
        CommanderEligibility.isLegalCommander(card) shouldBe false
    }

    test("planeswalker without override clause is not a legal commander") {
        val card = CardDefinition.planeswalker(
            name = "Jace, the Mind Sculptor",
            manaCost = ManaCost.parse("{2}{U}{U}"),
            subtypes = setOf(Subtype("Jace")),
            startingLoyalty = 3,
            oracleText = "+2: Look at the top card of target player's library.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe false
    }

    test("planeswalker with 'can be your commander' clause is a legal commander") {
        // Daretti, Scrap Savant — and several Commander-precon planeswalkers — have an explicit
        // override clause. The matcher is case-insensitive and doesn't require the card name to
        // appear in the same sentence (some printings phrase it as "This card can be your
        // commander").
        val card = CardDefinition.planeswalker(
            name = "Daretti, Scrap Savant",
            manaCost = ManaCost.parse("{4}{R}"),
            subtypes = setOf(Subtype("Daretti")),
            startingLoyalty = 3,
            oracleText = "+1: Draw two cards, then discard two cards.\nDaretti, Scrap Savant can be your commander.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("non-creature non-planeswalker with override clause is a legal commander") {
        // Faceless One — colorless changeling that explicitly says it can be your commander.
        // Modeled here as a vanilla creature with the override clause to verify the regex
        // matches even when no other branch fires.
        val card = CardDefinition(
            name = "Faceless One",
            manaCost = ManaCost.parse("{2}"),
            typeLine = TypeLine(
                cardTypes = setOf(CardType.CREATURE),
                subtypes = setOf(Subtype("Shapeshifter")),
            ),
            creatureStats = CreatureStats(2, 2),
            oracleText = "Changeling.\nFaceless One can be your commander.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }

    test("oracle text is matched case-insensitively") {
        val card = CardDefinition.planeswalker(
            name = "Test Planeswalker",
            manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype("Test")),
            startingLoyalty = 4,
            oracleText = "TEST PLANESWALKER CAN BE YOUR COMMANDER.",
        )
        CommanderEligibility.isLegalCommander(card) shouldBe true
    }
})
