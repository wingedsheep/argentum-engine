package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Drudge Skeletons reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DrudgeSkeletonsReprint = Printing(
    oracleId = "c180ed02-07f4-4538-9bea-8c249234a8e2",
    name = "Drudge Skeletons",
    setCode = "10E",
    collectorNumber = "139",
    artist = "Jim Nelson",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/01e71542-b9c1-4431-bfef-da629a3edc57.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
