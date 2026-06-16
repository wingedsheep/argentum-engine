package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plague Wind reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PlagueWindReprint = Printing(
    oracleId = "18ec721f-c1ac-4581-a61d-2f0b09d6bf92",
    name = "Plague Wind",
    setCode = "10E",
    collectorNumber = "169",
    artist = "Alan Pollack",
    imageUri = "https://cards.scryfall.io/normal/front/3/2/327d38a9-e631-4f5e-a7c3-b99794a40954.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
