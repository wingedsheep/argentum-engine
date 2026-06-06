package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Drudge Skeletons reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DrudgeSkeletonsReprint = Printing(
    oracleId = "c180ed02-07f4-4538-9bea-8c249234a8e2",
    name = "Drudge Skeletons",
    setCode = "8ED",
    collectorNumber = "129",
    artist = "Jim Nelson",
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c9fef91f-3044-4c6e-b090-c3819ee61b9c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
