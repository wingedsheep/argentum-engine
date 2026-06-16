package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Craw Wurm reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CrawWurmReprint = Printing(
    oracleId = "6a462a69-3e42-41de-a3aa-a488d9f38d69",
    name = "Craw Wurm",
    setCode = "LEB",
    collectorNumber = "191",
    artist = "Daniel Gelon",
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17d5c1c7-a882-479a-9077-0784e83b462d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
