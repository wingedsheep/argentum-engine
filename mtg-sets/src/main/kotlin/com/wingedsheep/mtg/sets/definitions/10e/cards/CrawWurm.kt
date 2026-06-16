package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Craw Wurm reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CrawWurmReprint = Printing(
    oracleId = "6a462a69-3e42-41de-a3aa-a488d9f38d69",
    name = "Craw Wurm",
    setCode = "10E",
    collectorNumber = "257",
    artist = "Richard Sardinha",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da6b8e11-b799-4235-bd8f-c4b55946c769.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
