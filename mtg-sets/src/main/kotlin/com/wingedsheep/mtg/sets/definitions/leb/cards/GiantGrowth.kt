package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Growth reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantGrowthReprint = Printing(
    oracleId = "5748ebf1-24e3-499d-ab7c-c2cebd462a24",
    name = "Giant Growth",
    setCode = "LEB",
    collectorNumber = "198",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/7/5/755a45bd-8fe6-4e4d-8065-024a2836751b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
