package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Horned Turtle
 * {2}{U}
 * Creature - Turtle
 * 1/4
 * (Vanilla creature)
 */
val HornedTurtle = card("Horned Turtle") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Turtle"
    power = 1
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7d25497-36b4-48b9-ba01-f24f6222d6be.jpg"
    }
}
