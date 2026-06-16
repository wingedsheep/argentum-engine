package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sulfurous Springs reprint in DMU.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in ICE's `cards/` package
 * (the card's earliest real printing). This file contributes only the DMU-specific
 * presentation row, surfaced via the set's `printings`.
 */
val SulfurousSpringsReprint = Printing(
    oracleId = "f5c38c01-4a40-469f-91a0-7479daf4e8e7",
    name = "Sulfurous Springs",
    setCode = "DMU",
    collectorNumber = "256",
    artist = "Bruce Brenneise",
    imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd5450a-6490-417f-9aea-b6fca6f380d7.jpg",
    releaseDate = "2022-09-09",
    rarity = Rarity.RARE,
)
