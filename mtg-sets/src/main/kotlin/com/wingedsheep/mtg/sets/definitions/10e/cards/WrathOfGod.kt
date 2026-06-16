package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrath of God reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WrathOfGodReprint = Printing(
    oracleId = "34515b16-c9a4-4f98-8c77-416a7a523407",
    name = "Wrath of God",
    setCode = "10E",
    collectorNumber = "61",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/b/9/b969f680-b176-4bfa-a160-714de8e03c25.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
