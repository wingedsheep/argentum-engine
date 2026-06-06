package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Searing Wind reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SearingWindReprint = Printing(
    oracleId = "b196045d-ece1-46ac-a647-b65f04225c10",
    name = "Searing Wind",
    setCode = "8ED",
    collectorNumber = "218",
    artist = "John Matson",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61afd3c5-7103-4247-8bb9-747532ae4265.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
