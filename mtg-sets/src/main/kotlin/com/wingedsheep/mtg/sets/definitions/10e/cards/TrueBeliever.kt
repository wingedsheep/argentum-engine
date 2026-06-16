package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * True Believer reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TrueBelieverReprint = Printing(
    oracleId = "5a0cd4dd-2212-4408-819e-0cb7c150418b",
    name = "True Believer",
    setCode = "10E",
    collectorNumber = "53",
    artist = "Alex Horley-Orlandelli",
    imageUri = "https://cards.scryfall.io/normal/front/8/5/85a9c5c4-47d1-4b87-8cbd-2212ee6c5887.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
