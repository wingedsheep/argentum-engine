package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Elite Archers reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EliteArchersReprint = Printing(
    oracleId = "2230ec18-4dcf-435e-b495-1905cb94e8b8",
    name = "Elite Archers",
    setCode = "8ED",
    collectorNumber = "18",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/b/2/b2341478-2a91-4ce8-881a-21e826b55c34.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
