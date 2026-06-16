package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Threaten reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThreatenReprint = Printing(
    oracleId = "3dc8d15a-3ca7-4d1f-a819-c1b7f947a59e",
    name = "Threaten",
    setCode = "10E",
    collectorNumber = "242",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/5/7/57d467d4-f7d2-403c-9b70-1b054b2c82bc.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
