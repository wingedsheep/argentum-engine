package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Honor Guard reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HonorGuardReprint = Printing(
    oracleId = "4ba82d8e-1857-40a9-8b42-5d8348c68859",
    name = "Honor Guard",
    setCode = "8ED",
    collectorNumber = "25",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/f/a/fa797080-aa3b-4285-a47a-878a3e8e584f.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
