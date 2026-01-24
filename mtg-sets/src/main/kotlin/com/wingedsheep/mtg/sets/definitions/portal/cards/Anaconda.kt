package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Anaconda
 * {3}{G}
 * Creature — Snake
 * 3/3
 * Swampwalk
 */
val Anaconda = card("Anaconda") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Snake"
    power = 3
    toughness = 3

    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Una Fricker"
        flavorText = "Silent death glides through the murky waters."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cd7e8a9-b0e1-f2a3-b4c5-d6e7f8a9b0c1.jpg"
    }
}
