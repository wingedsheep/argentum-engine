package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bottle Gnomes reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BottleGnomesReprint = Printing(
    oracleId = "54b5e429-7a44-480d-bea4-4f8eeb7449b5",
    name = "Bottle Gnomes",
    setCode = "10E",
    collectorNumber = "312",
    artist = "Ben Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/af7cc984-1378-4b01-8115-d3c5b4f68a2a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
