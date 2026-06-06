package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rampant Growth reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RampantGrowthReprint = Printing(
    oracleId = "8539f295-5d58-4436-a73a-b9277c4c7795",
    name = "Rampant Growth",
    setCode = "8ED",
    collectorNumber = "274",
    artist = "Tom Kyffin",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/01d56cde-e186-41e7-bcb2-80736a989678.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
