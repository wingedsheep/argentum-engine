package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Monastery Flock
 * {2}{U}
 * Creature — Bird
 * 0/5
 * Defender, flying
 * Morph {U}
 */
val MonasteryFlock = card("Monastery Flock") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Bird"
    power = 0
    toughness = 5
    oracleText = "Defender, flying\nMorph {U}"

    keywords(Keyword.DEFENDER, Keyword.FLYING)
    morph = "{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "John Avon"
        flavorText = "\"The arrow strikes one bird down, but the flock remains.\" —Jeskai teaching"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e53c0e50-4b0b-43d8-80c0-2c216722c87a.jpg?1562795087"
    }
}
