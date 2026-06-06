package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Solidarity reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SolidarityReprint = Printing(
    oracleId = "7b094a2b-2b69-4a9b-9829-c1aabc53f75e",
    name = "Solidarity",
    setCode = "8ED",
    collectorNumber = "46",
    artist = "John Zeleznik",
    imageUri = "https://cards.scryfall.io/normal/front/7/8/7810eec1-9bb4-480f-9074-712ad9eb8fba.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
