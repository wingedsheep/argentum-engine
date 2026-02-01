package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Glory Seeker
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 */
val GlorySeeker = card("Glory Seeker") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Dave Dorman"
        flavorText = "\"The turning of the tide always begins with one soldier's decision to head back into the fray.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9047075e-9fca-484d-bb79-32c0d6821281.jpg?1562929017"
    }
}
