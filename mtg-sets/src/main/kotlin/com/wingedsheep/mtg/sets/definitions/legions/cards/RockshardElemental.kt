package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rockshard Elemental
 * {5}{R}{R}
 * Creature — Elemental
 * 4/3
 * Double strike
 * Morph {4}{R}{R}
 */
val RockshardElemental = card("Rockshard Elemental") {
    manaCost = "{5}{R}{R}"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 3
    keywords(Keyword.DOUBLE_STRIKE)

    morph = "{4}{R}{R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d6343c0-3fb5-4bac-bea7-cba36498cd69.jpg?1562904235"
    }
}
