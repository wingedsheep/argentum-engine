package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Youthful Knight reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val YouthfulKnightReprint = Printing(
    oracleId = "ef2a24f5-ce5e-4054-843a-2cae0c66318a",
    name = "Youthful Knight",
    setCode = "10E",
    collectorNumber = "62",
    artist = "Rebecca Guay",
    imageUri = "https://cards.scryfall.io/normal/front/d/7/d7dbb437-9c76-4b87-99ba-1e24047dc3a6.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
