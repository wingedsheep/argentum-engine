package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shivan Dragon reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShivanDragonReprint = Printing(
    oracleId = "711eea87-0fa3-46e0-a42b-fa5a86455f04",
    name = "Shivan Dragon",
    setCode = "8ED",
    collectorNumber = "221",
    artist = "Donato Giancola",
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e9ac007d-1616-449c-92d5-1d321235e733.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
