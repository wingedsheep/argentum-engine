package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Think Twice reprint in FDN.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the FDN-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThinkTwiceReprint = Printing(
    oracleId = "fa85c5a2-8e83-4624-a35a-a0bbf17ecbb4",
    name = "Think Twice",
    setCode = "FDN",
    collectorNumber = "165",
    artist = "Anthony Francisco",
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d88faaa1-eb41-40f7-991c-5c06e1138f3d.jpg",
    releaseDate = "2024-11-15",
    rarity = Rarity.COMMON,
)
