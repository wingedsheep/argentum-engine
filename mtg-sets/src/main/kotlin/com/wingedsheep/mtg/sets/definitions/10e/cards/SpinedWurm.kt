package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spined Wurm reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpinedWurmReprint = Printing(
    oracleId = "05425f2f-7228-4bf5-8fe1-6fe99107e8e0",
    name = "Spined Wurm",
    setCode = "10E",
    collectorNumber = "298",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/e/8/e855e965-e01c-4203-bc5b-84646e99ac11.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
