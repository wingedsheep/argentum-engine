package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Swords reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfSwordsReprint = Printing(
    oracleId = "eb098958-50d3-4476-ba74-382033703ff9",
    name = "Wall of Swords",
    setCode = "10E",
    collectorNumber = "57",
    artist = "Zoltan Boros & Gabor Szikszai",
    imageUri = "https://cards.scryfall.io/normal/front/e/6/e6938602-d531-4ff1-abb3-c7982864c2e0.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
