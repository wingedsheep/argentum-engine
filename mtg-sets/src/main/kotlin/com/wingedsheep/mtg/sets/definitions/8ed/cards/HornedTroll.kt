package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Horned Troll reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HornedTrollReprint = Printing(
    oracleId = "6ecc61ff-719a-405d-b02a-01607729d05c",
    name = "Horned Troll",
    setCode = "8ED",
    collectorNumber = "257",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/f/f/fffce2f7-b619-4483-a75e-916343194641.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
