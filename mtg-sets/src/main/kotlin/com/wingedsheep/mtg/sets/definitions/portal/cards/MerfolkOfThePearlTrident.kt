package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merfolk of the Pearl Trident
 * {U}
 * Creature - Merfolk
 * 1/1
 * (Vanilla creature)
 */
val MerfolkOfThePearlTrident = card("Merfolk of the Pearl Trident") {
    manaCost = "{U}"
    typeLine = "Creature — Merfolk"
    power = 1
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/126fec7a-4f36-49e5-a2d7-96deb7af856f.jpg"
    }
}
