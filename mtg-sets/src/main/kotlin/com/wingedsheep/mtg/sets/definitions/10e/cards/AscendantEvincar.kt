package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ascendant Evincar reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AscendantEvincarReprint = Printing(
    oracleId = "e32c0de7-132d-4a39-85fb-da0ed1132878",
    name = "Ascendant Evincar",
    setCode = "10E",
    collectorNumber = "127",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/82419eea-a397-41df-b526-f3bea857bbbd.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
