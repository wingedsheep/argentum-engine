package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Catalog reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CatalogReprint = Printing(
    oracleId = "965139fb-2e58-4ef2-b639-3ceaa2995c00",
    name = "Catalog",
    setCode = "8ED",
    collectorNumber = "65",
    artist = "Berry",
    imageUri = "https://cards.scryfall.io/normal/front/0/b/0b50620d-430e-4c38-a95d-3d69a4c29ff9.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
