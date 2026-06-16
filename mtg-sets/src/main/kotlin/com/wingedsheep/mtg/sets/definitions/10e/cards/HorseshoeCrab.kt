package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Horseshoe Crab reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HorseshoeCrabReprint = Printing(
    oracleId = "793f29ac-8a5d-49bf-91e8-a32f771268f3",
    name = "Horseshoe Crab",
    setCode = "10E",
    collectorNumber = "87",
    artist = "Scott Kirschner",
    imageUri = "https://cards.scryfall.io/normal/front/a/3/a32862f4-53ed-41c7-b90f-5df2c01dd447.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
