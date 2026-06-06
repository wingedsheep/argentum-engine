package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Collective Unconscious reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CollectiveUnconsciousReprint = Printing(
    oracleId = "38abf769-e4ef-4b47-9c79-c8cda0b90f6c",
    name = "Collective Unconscious",
    setCode = "8ED",
    collectorNumber = "238",
    artist = "Andrew Goldhawk",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/ccce9141-6c73-4e33-a72c-caa87cea7888.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
