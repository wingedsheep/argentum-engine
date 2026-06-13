package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mox Emerald reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MoxEmeraldReprint = Printing(
    oracleId = "376ee366-e082-402f-b4db-6592fcfcacd2",
    name = "Mox Emerald",
    setCode = "LEB",
    collectorNumber = "262",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/e/a/ea5d9476-76be-48e7-b6a0-49ced25cb092.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
