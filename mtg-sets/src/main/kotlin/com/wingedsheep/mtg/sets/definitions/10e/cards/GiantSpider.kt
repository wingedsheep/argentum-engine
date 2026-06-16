package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Spider reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantSpiderReprint = Printing(
    oracleId = "e740ce2f-2134-473c-afa1-1b6d2d1e38ef",
    name = "Giant Spider",
    setCode = "10E",
    collectorNumber = "267",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/d/9/d9fefe43-144a-4f47-80ce-c627efed9ac6.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
