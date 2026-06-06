package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Spider reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantSpiderReprint = Printing(
    oracleId = "e740ce2f-2134-473c-afa1-1b6d2d1e38ef",
    name = "Giant Spider",
    setCode = "8ED",
    collectorNumber = "255",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/4/6/4632f3d9-9cdf-4871-9771-e8a8b1eaed52.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
