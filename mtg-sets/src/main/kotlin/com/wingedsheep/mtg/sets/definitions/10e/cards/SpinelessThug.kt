package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spineless Thug reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpinelessThugReprint = Printing(
    oracleId = "78bbd213-e8f4-40da-aef5-853244e95756",
    name = "Spineless Thug",
    setCode = "10E",
    collectorNumber = "180",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/0/2/0221a713-2fdc-4ff0-abe3-1d340d272232.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
