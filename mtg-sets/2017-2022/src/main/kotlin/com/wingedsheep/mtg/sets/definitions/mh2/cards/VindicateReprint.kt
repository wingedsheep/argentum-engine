package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vindicate reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Apocalypse (`apc`) `cards/` package; this file contributes only per-printing
 * presentation data.
 */
val VindicateReprint = Printing(
    oracleId = "63c1ac21-e3d8-40c2-8c09-3f31c52992ef",
    name = "Vindicate",
    setCode = "MH2",
    collectorNumber = "294",
    scryfallId = "683c4e13-525c-45c9-8832-bfe67965c34e",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/6/8/683c4e13-525c-45c9-8832-bfe67965c34e.jpg?1783926778",
    releaseDate = "2021-06-18",
    rarity = Rarity.RARE,
    borderColor = "black",
)
