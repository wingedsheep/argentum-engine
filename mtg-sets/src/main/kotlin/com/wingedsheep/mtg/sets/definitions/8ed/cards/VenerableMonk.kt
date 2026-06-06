package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Venerable Monk reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VenerableMonkReprint = Printing(
    oracleId = "5c5cfa6d-857f-44a9-80f7-46f016ef71e4",
    name = "Venerable Monk",
    setCode = "8ED",
    collectorNumber = "55",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bb78fad6-71d8-49a2-8e32-34675aaec796.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
