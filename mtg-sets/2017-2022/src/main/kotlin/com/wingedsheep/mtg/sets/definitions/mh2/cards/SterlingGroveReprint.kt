package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sterling Grove reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Invasion (`inv`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val SterlingGroveReprint = Printing(
    oracleId = "2c275a85-5a15-46cd-a6e7-add63f9b853d",
    name = "Sterling Grove",
    setCode = "MH2",
    collectorNumber = "293",
    scryfallId = "ba03e105-a76c-4769-a35a-d780448890ec",
    artist = "Seb McKinnon",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba03e105-a76c-4769-a35a-d780448890ec.jpg?1783926777",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)
