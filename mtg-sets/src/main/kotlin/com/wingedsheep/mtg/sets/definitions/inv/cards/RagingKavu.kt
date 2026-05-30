package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Kavu
 * {1}{R}{G}
 * Creature — Kavu
 * 3/1
 * Flash
 * Haste
 */
val RagingKavu = card("Raging Kavu") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Kavu"
    power = 3
    toughness = 1
    oracleText = "Flash\nHaste"

    keywords(Keyword.FLASH, Keyword.HASTE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Arnie Swekel"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27573679-e9e5-4bfc-b5d5-85d4648b01b6.jpg?1562903028"
    }
}
