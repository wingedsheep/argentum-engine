package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stone Rain reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StoneRainReprint = Printing(
    oracleId = "6e880df6-fc00-43d2-a9c8-f575f40b78c6",
    name = "Stone Rain",
    setCode = "8ED",
    collectorNumber = "225",
    artist = "John Matson",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/61bf7f43-c8db-417e-8600-3124f5fab097.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
