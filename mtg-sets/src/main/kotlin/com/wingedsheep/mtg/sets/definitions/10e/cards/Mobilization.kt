package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mobilization reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MobilizationReprint = Printing(
    oracleId = "6b8119e9-a9a9-4349-be32-adcb84b8eb79",
    name = "Mobilization",
    setCode = "10E",
    collectorNumber = "29",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/b/c/bccc50a9-2cd0-4ee0-b4c9-a181b6b0cc7f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
