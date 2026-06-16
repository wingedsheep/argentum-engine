package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hill Giant reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HillGiantReprint = Printing(
    oracleId = "342199e0-15b6-4824-83da-25caef2592b3",
    name = "Hill Giant",
    setCode = "10E",
    collectorNumber = "212",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/1/4/14c2be6a-9ca6-4d3a-8dd0-db4ea40799f8.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
