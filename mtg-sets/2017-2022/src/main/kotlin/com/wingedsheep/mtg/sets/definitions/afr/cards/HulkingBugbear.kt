package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hulking Bugbear
 * {1}{R}{R}
 * Creature — Goblin
 * 3/3
 * Haste
 */
val HulkingBugbear = card("Hulking Bugbear") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Haste"
    power = 3
    toughness = 3

    keywords(Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Filip Burburan"
        flavorText = "Bugbears move with surprising speed for their size and bulk, a fact that has led to the demise of many unwary adventurers."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f55d43d4-5f63-45c3-b8f8-0aebd23750a5.jpg?1783926476"
    }
}
