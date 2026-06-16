package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Robe of Mirrors reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RobeOfMirrorsReprint = Printing(
    oracleId = "093a20e5-ff14-41c7-b16c-1f745ddf6942",
    name = "Robe of Mirrors",
    setCode = "10E",
    collectorNumber = "101",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/9/9/9948fcc3-6e95-4b39-accd-a4083fff1244.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
