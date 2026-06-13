package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tropical Island reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TropicalIslandReprint = Printing(
    oracleId = "74b7fe23-5d3a-4092-8d78-7c0eba8f6f73",
    name = "Tropical Island",
    setCode = "LEB",
    collectorNumber = "284",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/a/c/ac19c5a1-ca13-4443-920b-83b567167ed4.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
