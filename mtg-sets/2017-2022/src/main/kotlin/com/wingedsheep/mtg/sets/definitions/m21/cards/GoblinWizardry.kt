package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Wizardry
 * {3}{R}
 * Instant
 * Create two 1/1 red Goblin Wizard creature tokens with prowess.
 */
val GoblinWizardry = card("Goblin Wizardry") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Create two 1/1 red Goblin Wizard creature tokens with prowess. (Whenever you cast " +
        "a noncreature spell, they get +1/+1 until end of turn.)"

    spell {
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin", "Wizard"),
            keywords = setOf(Keyword.PROWESS),
            imageUri = "https://cards.scryfall.io/normal/front/f/d/fd7094d6-6e23-47f2-a9da-4dd680294ac8.jpg?1783930592"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Izzy"
        flavorText = "\"Quiet in the library\" soon gave way to larger concerns."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7ac8bdd-851f-449d-a108-70578eabf254.jpg?1783930689"
        ruling("2022-06-10", "The tokens are named \"Goblin Wizard Token\" and have the creature types \"Goblin\" and \"Wizard.\"")
    }
}
