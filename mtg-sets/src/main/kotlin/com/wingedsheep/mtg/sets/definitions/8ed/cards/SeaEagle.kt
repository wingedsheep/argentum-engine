package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sea Eagle reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * S99's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SeaEagleReprint = Printing(
    oracleId = "acb57162-7093-4a3c-9818-d3b61ce757c6",
    name = "Sea Eagle",
    setCode = "8ED",
    collectorNumber = "S4",
    artist = "Anthony S. Waters",
    imageUri = "https://cards.scryfall.io/normal/front/7/e/7e8cacd1-51e7-48af-a7ae-48832dc34a92.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
