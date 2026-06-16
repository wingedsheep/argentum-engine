package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Grizzly Bears reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GrizzlyBearsReprint = Printing(
    oracleId = "14c8f55d-d177-4c25-a931-ebeb9e6062a0",
    name = "Grizzly Bears",
    setCode = "10E",
    collectorNumber = "268",
    artist = "D. J. Cleland-Hura",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/409f9b88-f03e-40b6-9883-68c14c37c0de.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
