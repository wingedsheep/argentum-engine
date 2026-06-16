package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Growth reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantGrowthReprint = Printing(
    oracleId = "5748ebf1-24e3-499d-ab7c-c2cebd462a24",
    name = "Giant Growth",
    setCode = "10E",
    collectorNumber = "266",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/d/4/d4db271b-b7f2-4a12-b606-2215bab4ac17.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
