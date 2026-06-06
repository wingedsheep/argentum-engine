package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tremor reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TremorReprint = Printing(
    oracleId = "4281a153-665b-44cf-bcff-9cf88c14c2b5",
    name = "Tremor",
    setCode = "8ED",
    collectorNumber = "228",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/7/d/7d47f27d-5a25-4d2a-b2a2-027b579b881b.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
