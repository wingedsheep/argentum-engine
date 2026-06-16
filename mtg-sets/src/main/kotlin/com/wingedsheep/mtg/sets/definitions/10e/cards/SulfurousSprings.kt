package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sulfurous Springs reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SulfurousSpringsReprint = Printing(
    oracleId = "f5c38c01-4a40-469f-91a0-7479daf4e8e7",
    name = "Sulfurous Springs",
    setCode = "10E",
    collectorNumber = "359",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/c/a/ca7f2426-5a56-4dca-a059-65aa7266ac83.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
