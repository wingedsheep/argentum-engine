package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spined Wurm reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpinedWurmReprint = Printing(
    oracleId = "05425f2f-7228-4bf5-8fe1-6fe99107e8e0",
    name = "Spined Wurm",
    setCode = "8ED",
    collectorNumber = "279",
    artist = "Keith Parkinson",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba620b1b-d333-443b-961a-25f65ca49a1e.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
