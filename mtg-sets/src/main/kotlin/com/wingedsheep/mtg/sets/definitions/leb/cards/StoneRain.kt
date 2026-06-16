package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stone Rain reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StoneRainReprint = Printing(
    oracleId = "6e880df6-fc00-43d2-a9c8-f575f40b78c6",
    name = "Stone Rain",
    setCode = "LEB",
    collectorNumber = "178",
    artist = "Daniel Gelon",
    imageUri = "https://cards.scryfall.io/normal/front/9/0/901831ad-1840-4287-b6a0-bea310598dc2.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
