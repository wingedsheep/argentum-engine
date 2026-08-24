package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Renegade Troops
 * {4}{R}
 * Creature — Human Soldier
 * 4/2
 * Haste
 */
val RenegadeTroops = card("Renegade Troops") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 4
    toughness = 2
    oracleText = "Haste"

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Liu Jianjian"
        flavorText = "\"Across the land rebellions seethed and swarmed / As vicious war lords swooped down on all sides.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a75095f4-a77f-4237-ae25-2e6f2f8788c1.jpg"
    }
}
