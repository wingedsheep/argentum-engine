package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wood Elves reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WoodElvesReprint = Printing(
    oracleId = "8973bd99-20f8-4867-90ef-50392147ee1b",
    name = "Wood Elves",
    setCode = "8ED",
    collectorNumber = "289",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da7aa861-556b-43a1-bf57-1efbbd438975.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
