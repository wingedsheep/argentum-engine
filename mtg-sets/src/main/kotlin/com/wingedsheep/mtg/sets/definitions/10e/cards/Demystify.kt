package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Demystify reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DemystifyReprint = Printing(
    oracleId = "fd591199-9f7a-4147-a150-13279dbb4498",
    name = "Demystify",
    setCode = "10E",
    collectorNumber = "14",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/6/1/6197c672-dfa1-4d1e-97ea-af916864d23c.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
