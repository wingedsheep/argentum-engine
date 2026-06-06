package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Storm Crow reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ALL's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StormCrowReprint = Printing(
    oracleId = "000d5588-5a4c-434e-988d-396632ade42c",
    name = "Storm Crow",
    setCode = "8ED",
    collectorNumber = "104",
    artist = "John Matson",
    imageUri = "https://cards.scryfall.io/normal/front/1/3/13f6106d-6822-47b5-956e-20e17143a818.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
