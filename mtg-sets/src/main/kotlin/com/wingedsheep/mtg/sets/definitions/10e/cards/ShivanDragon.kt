package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shivan Dragon reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShivanDragonReprint = Printing(
    oracleId = "711eea87-0fa3-46e0-a42b-fa5a86455f04",
    name = "Shivan Dragon",
    setCode = "10E",
    collectorNumber = "230",
    artist = "Donato Giancola",
    imageUri = "https://cards.scryfall.io/normal/front/0/d/0dcdd2db-1a4b-48dd-94cf-bd719ff40da9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
