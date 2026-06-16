package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Caves of Koilos reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CavesOfKoilosReprint = Printing(
    oracleId = "33de01e9-ce5a-42d4-afcb-343cd54a6d80",
    name = "Caves of Koilos",
    setCode = "10E",
    collectorNumber = "350",
    artist = "Jim Nelson",
    imageUri = "https://cards.scryfall.io/normal/front/a/c/ac3c4e31-673e-4148-bd2e-de569906ce7d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
