package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Afflict reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AfflictReprint = Printing(
    oracleId = "471f6ad3-cf01-42dd-a519-ea8e13039fea",
    name = "Afflict",
    setCode = "10E",
    collectorNumber = "125",
    artist = "Roger Raupp",
    imageUri = "https://cards.scryfall.io/normal/front/2/0/20f18447-2853-469b-b3b3-20d6bcafa6e7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
