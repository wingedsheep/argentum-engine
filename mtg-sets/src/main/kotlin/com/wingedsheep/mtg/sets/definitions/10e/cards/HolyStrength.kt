package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Holy Strength reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HolyStrengthReprint = Printing(
    oracleId = "9357de36-f8be-4f49-b2c8-9fe9eaf82b07",
    name = "Holy Strength",
    setCode = "10E",
    collectorNumber = "22",
    artist = "Terese Nielsen",
    imageUri = "https://cards.scryfall.io/normal/front/e/2/e21264cb-d915-4431-92fd-b0aa32a715fc.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
