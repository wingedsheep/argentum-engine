package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Verdant Force reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VerdantForceReprint = Printing(
    oracleId = "7a21ea22-3cd7-4c11-8895-5943c0d93a0d",
    name = "Verdant Force",
    setCode = "10E",
    collectorNumber = "307",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bbd3ac60-aae0-45e6-8592-32be316d2f14.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
