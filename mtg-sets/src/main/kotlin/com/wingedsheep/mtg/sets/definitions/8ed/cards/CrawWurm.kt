package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Craw Wurm reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CrawWurmReprint = Printing(
    oracleId = "6a462a69-3e42-41de-a3aa-a488d9f38d69",
    name = "Craw Wurm",
    setCode = "8ED",
    collectorNumber = "239",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/9/e/9edc5392-0529-471c-858f-5602aabb626c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
