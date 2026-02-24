package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alabaster Kirin
 * {3}{W}
 * Creature — Kirin
 * 2/3
 * Flying, vigilance
 */
val AlabasterKirin = card("Alabaster Kirin") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Kirin"
    power = 2
    toughness = 3
    oracleText = "Flying, vigilance"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Igor Kieryluk"
        flavorText = "The appearance of a kirin signifies the passing or arrival of an important figure. As word of sightings spread, all the khans took it to mean themselves. Only the shaman Chianul thought of Sarkhan Vol."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad1ce529-06ed-4e85-9988-8c8b58401ed5.jpg?1562791877"
    }
}
