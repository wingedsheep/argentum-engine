package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Krenko's Command
 * {1}{R}
 * Sorcery
 *
 * Create two 1/1 red Goblin creature tokens.
 */
val KrenkosCommand = card("Krenko's Command") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 red Goblin creature tokens."

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/0/e/0e67efea-8a80-42ec-8e77-07d387d933d4.jpg?1783940442",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Karl Kopinski"
        flavorText = "Goblins are eager to follow orders, especially when those orders involve stealing, " +
            "hurting, annoying, eating, destroying, or swearing."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84df41e9-e973-4441-b17f-434517134d46.jpg?1783940481"
    }
}
