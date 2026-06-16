package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Highway Robber reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HighwayRobberReprint = Printing(
    oracleId = "5de0769e-5141-42d5-aaa6-549cfc154de8",
    name = "Highway Robber",
    setCode = "10E",
    collectorNumber = "150",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/b/9/b9d4d095-664e-4704-b922-f868d0d454d3.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
