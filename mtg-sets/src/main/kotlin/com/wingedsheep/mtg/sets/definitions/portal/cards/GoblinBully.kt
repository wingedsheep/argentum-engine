package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Bully
 * {1}{R}
 * Creature — Goblin
 * 2/1
 */
val GoblinBully = card("Goblin Bully") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Pete Venters"
        flavorText = "It's easy to stand head and shoulders over those with no backbone."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1b2c3d4-e5f6-a7b8-c9d0-e1f2a3b4c5d6.jpg"
    }
}
