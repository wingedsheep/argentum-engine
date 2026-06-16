package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reya Dawnbringer reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ReyaDawnbringerReprint = Printing(
    oracleId = "08d1a5d1-141d-48e3-ac22-c2529191cb13",
    name = "Reya Dawnbringer",
    setCode = "10E",
    collectorNumber = "35",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/4/b/4b81ef83-41c2-46dc-aff9-41bb3a32637a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
