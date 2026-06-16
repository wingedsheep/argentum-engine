package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Fire reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfFireReprint = Printing(
    oracleId = "f38c8b47-e8e0-4d2f-b1da-d8d986805a48",
    name = "Wall of Fire",
    setCode = "10E",
    collectorNumber = "247",
    artist = "Dan Dos Santos",
    imageUri = "https://cards.scryfall.io/normal/front/6/9/69e461bb-5d51-4f9c-a753-9fb090f564e8.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
