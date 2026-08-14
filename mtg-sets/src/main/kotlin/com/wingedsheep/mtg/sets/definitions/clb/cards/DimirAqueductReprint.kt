package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dimir Aqueduct reprint in Commander Legends: Battle for Baldur's Gate. The canonical
 * [com.wingedsheep.sdk.model.CardDefinition] lives in the Ravnica: City of Guilds (`rav`)
 * `cards/` package (the card's earliest real printing); this file contributes only
 * per-printing presentation data.
 */
val DimirAqueductReprint = Printing(
    oracleId = "378a1d57-e2f1-4b84-9692-1564602e9e99",
    name = "Dimir Aqueduct",
    setCode = "CLB",
    collectorNumber = "891",
    scryfallId = "3a7552d5-ec37-4599-8789-fe20b6d4b7bf",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/3/a/3a7552d5-ec37-4599-8789-fe20b6d4b7bf.jpg?1783922377",
    releaseDate = "2022-06-10",
    rarity = Rarity.UNCOMMON,
)
