package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Naturalize reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NaturalizeReprint = Printing(
    oracleId = "bdb3ca68-ec1f-4e16-81cc-d23f8f52c728",
    name = "Naturalize",
    setCode = "8ED",
    collectorNumber = "270",
    artist = "Tim Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/7/e/7e1e905d-a911-40a7-a793-8535e840230e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
