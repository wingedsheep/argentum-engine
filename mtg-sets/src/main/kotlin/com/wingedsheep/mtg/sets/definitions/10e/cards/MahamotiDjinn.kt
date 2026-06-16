package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mahamoti Djinn reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MahamotiDjinnReprint = Printing(
    oracleId = "c39ea5f9-6ec0-4697-897b-779e326754a7",
    name = "Mahamoti Djinn",
    setCode = "10E",
    collectorNumber = "90",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7a79bb42-a4c4-46c6-a5ed-43bbf028a7f0.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
