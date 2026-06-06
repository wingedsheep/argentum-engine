package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flying Carpet reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ARN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlyingCarpetReprint = Printing(
    oracleId = "453524fb-2ffa-421f-883c-b77ae2850c52",
    name = "Flying Carpet",
    setCode = "8ED",
    collectorNumber = "301",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52e5bc70-663f-41a5-a8d1-487c57a6f029.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
