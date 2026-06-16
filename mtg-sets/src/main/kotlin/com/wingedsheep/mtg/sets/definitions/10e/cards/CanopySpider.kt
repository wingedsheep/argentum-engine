package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Canopy Spider reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CanopySpiderReprint = Printing(
    oracleId = "37f3733e-cc4e-4d84-b29b-d474f6e254a2",
    name = "Canopy Spider",
    setCode = "10E",
    collectorNumber = "254",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/c/a/ca3d403b-9b86-46f6-8a82-a69c754984e0.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
