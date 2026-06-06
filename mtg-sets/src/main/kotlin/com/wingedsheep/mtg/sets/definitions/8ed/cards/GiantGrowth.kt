package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Growth reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantGrowthReprint = Printing(
    oracleId = "5748ebf1-24e3-499d-ab7c-c2cebd462a24",
    name = "Giant Growth",
    setCode = "8ED",
    collectorNumber = "254",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/1/2/1210c342-1473-44ae-bcaa-e6b3ef9aea90.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
