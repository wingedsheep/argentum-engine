package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Lyrist reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ElvishLyristReprint = Printing(
    oracleId = "e0de1c9b-f71f-4498-a968-d3c635cb4c5c",
    name = "Elvish Lyrist",
    setCode = "8ED",
    collectorNumber = "242",
    artist = "Michael Koelsch",
    imageUri = "https://cards.scryfall.io/normal/front/4/a/4a1da8da-12b2-47aa-9565-92d305061798.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
