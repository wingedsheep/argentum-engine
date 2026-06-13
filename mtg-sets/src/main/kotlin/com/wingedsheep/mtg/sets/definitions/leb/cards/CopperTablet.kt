package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Copper Tablet reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CopperTabletReprint = Printing(
    oracleId = "16d1023b-2162-4010-8bf4-218dbe7c99a0",
    name = "Copper Tablet",
    setCode = "LEB",
    collectorNumber = "239",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/9/3/93842064-a0a8-4e4d-9c8a-e8a86448d225.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
