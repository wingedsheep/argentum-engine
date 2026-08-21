package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Zuran Orb reprint in Modern Horizons 2. The canonical [com.wingedsheep.sdk.model.CardDefinition]
 * lives in the Ice Age (`ice`) `cards/` package; this file contributes only
 * per-printing presentation data.
 */
val ZuranOrbReprint = Printing(
    oracleId = "08cb8a30-9cb4-4517-bee5-8848aa60d1a2",
    name = "Zuran Orb",
    setCode = "MH2",
    collectorNumber = "300",
    scryfallId = "618c8ecc-686d-41de-b9b1-1a7ee9cc7c14",
    artist = "Ryan Pancoast",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/618c8ecc-686d-41de-b9b1-1a7ee9cc7c14.jpg?1783926775",
    releaseDate = "2021-06-18",
    rarity = Rarity.UNCOMMON,
    borderColor = "black",
)
