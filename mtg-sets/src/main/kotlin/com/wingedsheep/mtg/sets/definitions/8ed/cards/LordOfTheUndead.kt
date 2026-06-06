package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lord of the Undead reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PLS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LordOfTheUndeadReprint = Printing(
    oracleId = "7714af0e-41d9-4609-967f-27233b46055f",
    name = "Lord of the Undead",
    setCode = "8ED",
    collectorNumber = "141",
    artist = "Brom",
    imageUri = "https://cards.scryfall.io/normal/front/2/7/27d4b539-0e45-49ca-96fa-5df1ccb3b235.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
