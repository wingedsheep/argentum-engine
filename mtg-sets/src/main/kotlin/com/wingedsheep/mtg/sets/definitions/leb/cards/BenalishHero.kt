package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Benalish Hero reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BenalishHeroReprint = Printing(
    oracleId = "4c81cfb7-8765-4e28-ae33-4287fa9a86cc",
    name = "Benalish Hero",
    setCode = "LEB",
    collectorNumber = "4",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/f/6/f62c68d0-9b1e-4abe-991d-a645effeb676.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
