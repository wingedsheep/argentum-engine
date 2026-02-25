package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ridgetop Raptor
 * {3}{R}
 * Creature — Dinosaur Beast
 * 2/1
 * Double strike
 *
 * Oracle errata: Original type "Creature — Beast" updated to "Creature — Dinosaur Beast"
 * in the Ixalan creature type update.
 */
val RidgetopRaptor = card("Ridgetop Raptor") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Dinosaur Beast"
    power = 2
    toughness = 1
    keywords(Keyword.DOUBLE_STRIKE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Daren Bader"
        flavorText = "The Skirk Ridge has many wonderful things to discover—like escape routes."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1013cbc4-09f4-484f-b328-9f7403225149.jpg?1562898258"
    }
}
