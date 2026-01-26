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
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a2012ad-6425-4935-83af-fc7309ec2ece.jpg"
    }
}
