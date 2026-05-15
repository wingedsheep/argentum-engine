package com.wingedsheep.mtg.sets.definitions.lea.cards

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
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 4

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da237992-2919-4e37-8f56-2164095f59b5.jpg?1559591353"
    }
}
