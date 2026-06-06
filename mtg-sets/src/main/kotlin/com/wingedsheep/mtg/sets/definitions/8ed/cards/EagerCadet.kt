package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Eager Cadet reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * S99's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EagerCadetReprint = Printing(
    oracleId = "39780477-01ba-40c6-8fe6-2178efb10b92",
    name = "Eager Cadet",
    setCode = "8ED",
    collectorNumber = "S1",
    artist = "Greg Hildebrandt & Tim Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/8/1/81a5ec5a-b490-456a-98e8-36cf4b836d2a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
