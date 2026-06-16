package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Flight reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FlightReprint = Printing(
    oracleId = "6a4068b0-fb4f-429c-a94e-47849f3eb7ef",
    name = "Flight",
    setCode = "LEB",
    collectorNumber = "59",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/2/4/24584ffa-8ed1-4930-b6d8-ac1d02738ed0.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
