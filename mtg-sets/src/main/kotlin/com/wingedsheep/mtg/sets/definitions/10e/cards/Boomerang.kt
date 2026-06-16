package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boomerang reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BoomerangReprint = Printing(
    oracleId = "dc4a4996-108a-4aac-850f-2d9f76403446",
    name = "Boomerang",
    setCode = "10E",
    collectorNumber = "70",
    artist = "Arnie Swekel",
    imageUri = "https://cards.scryfall.io/normal/front/f/4/f4095675-1e3f-4d3c-b565-5c434d2e5cc0.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
