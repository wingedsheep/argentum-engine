package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gurmag Swiftwing
 * {1}{B}
 * Creature — Bat
 * 1/2
 * Flying, first strike, haste
 */
val GurmagSwiftwing = card("Gurmag Swiftwing") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Bat"
    power = 1
    toughness = 2
    oracleText = "Flying, first strike, haste"

    keywords(Keyword.FLYING, Keyword.FIRST_STRIKE, Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Jeff Simpson"
        flavorText = "\"Anything a falcon can do, a bat can do in pitch darkness.\"\n—Urdnan the Wanderer"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7492918-b3c6-468f-9484-d73f0a27b37b.jpg?1562791583"
    }
}
