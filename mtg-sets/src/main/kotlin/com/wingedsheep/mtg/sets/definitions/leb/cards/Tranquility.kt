package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tranquility reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TranquilityReprint = Printing(
    oracleId = "f671e3c3-cd59-4d06-a1af-5d04892cf74d",
    name = "Tranquility",
    setCode = "LEB",
    collectorNumber = "221",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee21b620-4dfa-4e06-872e-8d8ffce12f76.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
