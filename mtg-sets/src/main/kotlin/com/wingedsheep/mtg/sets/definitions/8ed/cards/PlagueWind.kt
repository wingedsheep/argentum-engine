package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plague Wind reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PlagueWindReprint = Printing(
    oracleId = "18ec721f-c1ac-4581-a61d-2f0b09d6bf92",
    name = "Plague Wind",
    setCode = "8ED",
    collectorNumber = "155",
    artist = "Alan Pollack",
    imageUri = "https://cards.scryfall.io/normal/front/0/5/056be80b-54df-4a9e-8ccb-1e576a001d7c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
