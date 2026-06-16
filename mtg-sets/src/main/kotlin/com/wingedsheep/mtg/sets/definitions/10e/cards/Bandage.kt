package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bandage reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * STH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BandageReprint = Printing(
    oracleId = "c664df64-0103-462b-a0f8-c483b152974a",
    name = "Bandage",
    setCode = "10E",
    collectorNumber = "9",
    artist = "Rebecca Guay",
    imageUri = "https://cards.scryfall.io/normal/front/c/9/c9c17e3b-4d7f-4472-afe1-8e9358b82f2c.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
