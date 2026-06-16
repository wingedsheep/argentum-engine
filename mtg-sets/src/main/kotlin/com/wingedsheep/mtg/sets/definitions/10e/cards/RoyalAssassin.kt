package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Royal Assassin reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RoyalAssassinReprint = Printing(
    oracleId = "9ed6f28f-a3db-48c5-9ab0-b90a7fba5f57",
    name = "Royal Assassin",
    setCode = "10E",
    collectorNumber = "174",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/1/b/1bc1d6b5-ee32-4d62-848d-884da6376c63.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
