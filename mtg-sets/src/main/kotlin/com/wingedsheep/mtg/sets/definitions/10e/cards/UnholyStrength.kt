package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unholy Strength reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UnholyStrengthReprint = Printing(
    oracleId = "090d88a9-7f2d-4bd1-a30a-7c48d05068be",
    name = "Unholy Strength",
    setCode = "10E",
    collectorNumber = "185",
    artist = "Terese Nielsen",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8adc48d8-7ff9-46d4-921a-03e63ca41a66.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
