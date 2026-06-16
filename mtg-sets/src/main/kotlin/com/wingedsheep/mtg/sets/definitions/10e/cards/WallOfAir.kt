package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Air reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfAirReprint = Printing(
    oracleId = "b2d3da40-e2f7-4480-9da9-33019d6f4071",
    name = "Wall of Air",
    setCode = "10E",
    collectorNumber = "124",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/40595366-8601-40e0-a070-fc218723270d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
