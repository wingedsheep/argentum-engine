package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrath of God reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WrathOfGodReprint = Printing(
    oracleId = "34515b16-c9a4-4f98-8c77-416a7a523407",
    name = "Wrath of God",
    setCode = "LEB",
    collectorNumber = "46",
    artist = "Quinton Hoover",
    imageUri = "https://cards.scryfall.io/normal/front/9/6/96dd2d61-a43d-4582-b730-71d4fac0fa23.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
