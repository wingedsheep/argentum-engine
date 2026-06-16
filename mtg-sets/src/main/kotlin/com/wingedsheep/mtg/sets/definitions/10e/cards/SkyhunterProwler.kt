package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skyhunter Prowler reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SkyhunterProwlerReprint = Printing(
    oracleId = "5e0d13b0-d5d0-4c5f-945e-5cda7d980670",
    name = "Skyhunter Prowler",
    setCode = "10E",
    collectorNumber = "42",
    artist = "Vance Kovacs",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52aa4af5-f0cb-4512-bef5-2e46a43aa27b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
