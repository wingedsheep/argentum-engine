package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sage Owl reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * WTH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SageOwlReprint = Printing(
    oracleId = "d47fc902-51f7-4ad1-8ee4-c973e32192b8",
    name = "Sage Owl",
    setCode = "8ED",
    collectorNumber = "98",
    artist = "Mark Brill",
    imageUri = "https://cards.scryfall.io/normal/front/c/2/c21f6c39-f879-452f-a643-f503d88206d6.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
