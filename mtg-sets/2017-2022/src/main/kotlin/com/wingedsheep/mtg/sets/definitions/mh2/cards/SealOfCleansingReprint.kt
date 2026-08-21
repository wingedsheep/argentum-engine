package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Seal of Cleansing reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Nemesis (`nem`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val SealOfCleansingReprint = Printing(
    oracleId = "a75dbe70-7e3e-446f-9a76-9fbb414f2e7c",
    name = "Seal of Cleansing",
    setCode = "MH2",
    collectorNumber = "264",
    scryfallId = "27885a9a-3bcd-476d-97a9-acaec0553a60",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/2/7/27885a9a-3bcd-476d-97a9-acaec0553a60.jpg?1783926790",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)
