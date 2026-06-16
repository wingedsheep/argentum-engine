package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Scoria Wurm reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ScoriaWurmReprint = Printing(
    oracleId = "ee45c80c-699c-4e37-97e6-1859c6601c99",
    name = "Scoria Wurm",
    setCode = "10E",
    collectorNumber = "227",
    artist = "Steve Firchow",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/406f7fef-00f0-4ca5-ada4-c4a760e68467.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
