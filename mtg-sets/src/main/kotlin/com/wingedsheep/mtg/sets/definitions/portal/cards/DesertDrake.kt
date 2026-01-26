package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Desert Drake
 * {3}{R}
 * Creature — Drake
 * 2/2
 * Flying
 */
val DesertDrake = card("Desert Drake") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Roger Raupp"
        flavorText = "It hunts the scorching winds."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24673b35-aed2-40c0-a4ae-93bc4d392562.jpg"
    }
}
