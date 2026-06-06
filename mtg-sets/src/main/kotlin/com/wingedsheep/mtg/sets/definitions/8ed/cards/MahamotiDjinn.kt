package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mahamoti Djinn reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MahamotiDjinnReprint = Printing(
    oracleId = "c39ea5f9-6ec0-4697-897b-779e326754a7",
    name = "Mahamoti Djinn",
    setCode = "8ED",
    collectorNumber = "88",
    artist = "Eric Peterson",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c481454e-9544-4b8e-a1bc-d0bbc9fc64ef.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
