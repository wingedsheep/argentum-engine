package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sage Owl reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * WTH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SageOwlReprint = Printing(
    oracleId = "d47fc902-51f7-4ad1-8ee4-c973e32192b8",
    name = "Sage Owl",
    setCode = "10E",
    collectorNumber = "104",
    artist = "Mark Brill",
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1ed89567-cb18-4e51-a978-eac81d112aa1.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
