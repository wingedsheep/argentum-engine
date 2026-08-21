package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Seal of Removal reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Nemesis (`nem`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val SealOfRemovalReprint = Printing(
    oracleId = "f0801029-bcf7-4bdb-84bf-e88dcaa9dc03",
    name = "Seal of Removal",
    setCode = "MH2",
    collectorNumber = "269",
    scryfallId = "6dfc7060-e374-486f-8029-d3fdfe4b61e7",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/6/d/6dfc7060-e374-486f-8029-d3fdfe4b61e7.jpg?1783926788",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)
