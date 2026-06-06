package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Inferno reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DRK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val InfernoReprint = Printing(
    oracleId = "69e2df4e-c7f3-4c3a-be5b-1c4afb29cead",
    name = "Inferno",
    setCode = "8ED",
    collectorNumber = "196",
    artist = "Don Hazeltine",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bf6895de-9c74-49a0-a71c-1e55a6897dfa.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
