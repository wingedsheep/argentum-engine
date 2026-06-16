package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Venerable Monk reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VenerableMonkReprint = Printing(
    oracleId = "5c5cfa6d-857f-44a9-80f7-46f016ef71e4",
    name = "Venerable Monk",
    setCode = "10E",
    collectorNumber = "55",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/b/3/b36501d5-4f45-4191-b8ec-6ab435b61bc1.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
