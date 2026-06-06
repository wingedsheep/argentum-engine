package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Horned Turtle reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HornedTurtleReprint = Printing(
    oracleId = "7a4ae76b-6241-4fa8-b239-5ec941a0e7df",
    name = "Horned Turtle",
    setCode = "8ED",
    collectorNumber = "83",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/af3cb1a4-2552-4575-be8d-bef9d721cfc8.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
