package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tidal Kraken reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TidalKrakenReprint = Printing(
    oracleId = "104a8f76-d178-4a33-bb59-e8767da3d982",
    name = "Tidal Kraken",
    setCode = "8ED",
    collectorNumber = "108",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/0/8/08147258-2319-49d5-bfb1-cc4f817d9c72.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
