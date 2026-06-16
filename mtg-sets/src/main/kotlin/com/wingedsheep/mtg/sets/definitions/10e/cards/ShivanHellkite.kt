package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shivan Hellkite reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShivanHellkiteReprint = Printing(
    oracleId = "069be6a0-9545-4fdc-ad40-957fe307db7a",
    name = "Shivan Hellkite",
    setCode = "10E",
    collectorNumber = "231",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03993ba0-cdf1-4170-ab4e-7d1378aede12.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
