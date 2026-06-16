package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cloud Elemental reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CloudElementalReprint = Printing(
    oracleId = "0b751cc7-3a19-4207-9e64-83e18d279588",
    name = "Cloud Elemental",
    setCode = "10E",
    collectorNumber = "74",
    artist = "Michael Sutfin",
    imageUri = "https://cards.scryfall.io/normal/front/c/6/c6bc3025-704f-476b-bcb3-74b905755dc9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
