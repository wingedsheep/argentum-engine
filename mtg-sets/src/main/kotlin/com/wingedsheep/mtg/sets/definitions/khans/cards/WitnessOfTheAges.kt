package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Witness of the Ages
 * {6}
 * Artifact Creature — Golem
 * 4/4
 * Morph {5} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 */
val WitnessOfTheAges = card("Witness of the Ages") {
    manaCost = "{6}"
    typeLine = "Artifact Creature — Golem"
    power = 4
    toughness = 4
    oracleText = "Morph {5}"

    morph = "{5}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Izzy"
        flavorText = "It strode through the clash of dragons, the fall of Ugin, and the rise of the khans."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/763ac4c4-af2d-4b71-9fb3-244c96f93860.jpg?1562788742"
    }
}
