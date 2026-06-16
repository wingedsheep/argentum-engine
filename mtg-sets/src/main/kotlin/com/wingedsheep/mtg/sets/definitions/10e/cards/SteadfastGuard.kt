package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Steadfast Guard reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SteadfastGuardReprint = Printing(
    oracleId = "0c3b3c04-017d-4741-90c8-4b6c0bd46e49",
    name = "Steadfast Guard",
    setCode = "10E",
    collectorNumber = "48",
    artist = "Michael Komarck",
    imageUri = "https://cards.scryfall.io/normal/front/4/2/42a5c12b-c947-4a71-b54f-e310150858a3.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
