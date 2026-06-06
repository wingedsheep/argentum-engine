package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flight reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlightReprint = Printing(
    oracleId = "6a4068b0-fb4f-429c-a94e-47849f3eb7ef",
    name = "Flight",
    setCode = "8ED",
    collectorNumber = "80",
    artist = "Arnie Swekel",
    imageUri = "https://cards.scryfall.io/normal/front/4/5/45a7d379-3fb5-40eb-ba3c-7b51b7c5f57e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
