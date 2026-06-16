package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Honor Guard reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HonorGuardReprint = Printing(
    oracleId = "4ba82d8e-1857-40a9-8b42-5d8348c68859",
    name = "Honor Guard",
    setCode = "10E",
    collectorNumber = "23",
    artist = "Dan Dos Santos",
    imageUri = "https://cards.scryfall.io/normal/front/9/c/9c8a5add-87af-447d-9fd7-c4aeec3685fb.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
