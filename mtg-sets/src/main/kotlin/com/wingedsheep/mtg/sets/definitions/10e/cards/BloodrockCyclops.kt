package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodrock Cyclops reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * WTH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BloodrockCyclopsReprint = Printing(
    oracleId = "06a3fe24-7e1f-4f98-ba2f-dbb5d38f4b9d",
    name = "Bloodrock Cyclops",
    setCode = "10E",
    collectorNumber = "192",
    artist = "Alex Horley-Orlandelli",
    imageUri = "https://cards.scryfall.io/normal/front/e/3/e3a830f8-b508-431e-aadc-36330dcfe5db.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
