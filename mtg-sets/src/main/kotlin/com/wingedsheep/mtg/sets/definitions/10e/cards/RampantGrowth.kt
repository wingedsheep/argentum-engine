package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rampant Growth reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RampantGrowthReprint = Printing(
    oracleId = "8539f295-5d58-4436-a73a-b9277c4c7795",
    name = "Rampant Growth",
    setCode = "10E",
    collectorNumber = "288",
    artist = "Steven Belledin",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee662f0a-f874-464c-8206-d4ff427daf2b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
