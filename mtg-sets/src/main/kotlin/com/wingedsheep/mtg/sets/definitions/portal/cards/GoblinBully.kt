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
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2094f2f6-8b38-47d6-973d-271986b5d982.jpg"
    }
}
