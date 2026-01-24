package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Djinn of the Lamp
 * {5}{U}{U}
 * Creature - Djinn
 * 5/6
 * Flying
 */
val DjinnOfTheLamp = card("Djinn of the Lamp") {
    manaCost = "{5}{U}{U}"
    typeLine = "Creature — Djinn"
    power = 5
    toughness = 6

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a5e7b52-2663-4140-9758-f24b8b947876.jpg"
    }
}
