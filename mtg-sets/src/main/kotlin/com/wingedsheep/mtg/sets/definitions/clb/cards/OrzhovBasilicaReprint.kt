package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Orzhov Basilica reprint in Commander Legends: Battle for Baldur's Gate. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Guildpact (`gpt`) `cards/` package
 * (the card's earliest real printing); this file contributes only per-printing presentation data.
 */
val OrzhovBasilicaReprint = Printing(
    oracleId = "aa00ae0b-7c0f-427e-8102-ce0e2a6af5df",
    name = "Orzhov Basilica",
    setCode = "CLB",
    collectorNumber = "906",
    scryfallId = "61ed0bb4-91b8-4144-b3d6-f92d6821d979",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61ed0bb4-91b8-4144-b3d6-f92d6821d979.jpg?1783922370",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
