package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Grizzly Bears reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GrizzlyBearsReprint = Printing(
    oracleId = "14c8f55d-d177-4c25-a931-ebeb9e6062a0",
    name = "Grizzly Bears",
    setCode = "8ED",
    collectorNumber = "256",
    artist = "D. J. Cleland-Hura",
    imageUri = "https://cards.scryfall.io/normal/front/7/2/72d5d8b0-e22d-4bb3-9e36-5038601d22f7.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
