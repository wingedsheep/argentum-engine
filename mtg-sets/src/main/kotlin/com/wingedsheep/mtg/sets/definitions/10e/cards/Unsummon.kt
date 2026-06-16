package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unsummon reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UnsummonReprint = Printing(
    oracleId = "837182db-1bf3-4a2c-bd01-1af9d9873561",
    name = "Unsummon",
    setCode = "10E",
    collectorNumber = "122",
    artist = "Ron Spencer",
    imageUri = "https://cards.scryfall.io/normal/front/e/b/ebe16e3c-9cf5-4b5a-8ec9-8a2d60f3fcc9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
