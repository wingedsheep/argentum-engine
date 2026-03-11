package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fire Elemental
 * {3}{R}{R}
 * Creature — Elemental
 * 5/4
 */
val FireElemental = card("Fire Elemental") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 4

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Joe Slucher"
        flavorText = "\"The best way to learn from a book on pyromancy is to burn it.\" —Jaya Ballard"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62405700-d42c-4c96-9678-dd72d7b7c807.jpg?1562736683"
    }
}
