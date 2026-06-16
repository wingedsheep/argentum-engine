package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Underground River reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UndergroundRiverReprint = Printing(
    oracleId = "857febd9-cdd7-4f8e-a852-d88084b0cfbc",
    name = "Underground River",
    setCode = "10E",
    collectorNumber = "362",
    artist = "Andrew Goldhawk",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c298bf7-2514-43d4-a285-1386d6ba0835.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
