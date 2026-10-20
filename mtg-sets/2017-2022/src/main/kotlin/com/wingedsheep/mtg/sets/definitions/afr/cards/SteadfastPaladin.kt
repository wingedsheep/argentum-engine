package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Steadfast Paladin
 * {1}{W}
 * Creature — Dwarf Knight
 * 2/2
 * Lifelink
 */
val SteadfastPaladin = card("Steadfast Paladin") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Knight"
    oracleText = "Lifelink"
    power = 2
    toughness = 2
    keywords(Keyword.LIFELINK)
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Chris Rallis"
        flavorText = "\"When I made my oath to Moradin, I left selfishness and cowardice behind. Every battle, I make that commitment anew.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2c06b6e-faf5-467d-bfe1-9ff00be9e2f5.jpg?1783926524"
    }
}
