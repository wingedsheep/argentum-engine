package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ainok Tracker
 * {5}{R}
 * Creature — Dog Scout
 * 3/3
 * First strike
 * Morph {4}{R}
 */
val AinokTracker = card("Ainok Tracker") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Dog Scout"
    power = 3
    toughness = 3
    oracleText = "First strike\nMorph {4}{R}"

    keywords(Keyword.FIRST_STRIKE)
    morph = "{4}{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Evan Shipard"
        flavorText = "Some ainok of the mountains are accepted among the Temur as trusted hunt-mates."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0ff9400d-4842-471f-bf7e-3b21df352e0a.jpg?1562782629"
    }
}
