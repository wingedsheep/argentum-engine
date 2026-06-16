package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Thundering Giant reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThunderingGiantReprint = Printing(
    oracleId = "d23329e2-4134-4e49-b9b6-5febef2bce31",
    name = "Thundering Giant",
    setCode = "10E",
    collectorNumber = "243",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1f6b8f09-f021-4ac8-900f-daeac527477e.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
