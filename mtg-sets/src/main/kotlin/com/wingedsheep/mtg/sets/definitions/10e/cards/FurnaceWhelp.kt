package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Furnace Whelp reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FurnaceWhelpReprint = Printing(
    oracleId = "8422f1e0-00ca-4ffb-a6b2-e2c9f96d7f23",
    name = "Furnace Whelp",
    setCode = "10E",
    collectorNumber = "205",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/4/c/4c00aa95-3ef3-4639-bda2-6593515f68ae.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
