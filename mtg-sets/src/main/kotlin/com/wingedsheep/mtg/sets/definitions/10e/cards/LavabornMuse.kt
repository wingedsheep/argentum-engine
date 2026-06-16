package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lavaborn Muse reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LavabornMuseReprint = Printing(
    oracleId = "2825ff5a-5340-4494-bfde-80f6f5bce5a2",
    name = "Lavaborn Muse",
    setCode = "10E",
    collectorNumber = "216",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/7/4/74adb3f7-2453-4133-a08b-4a2b1dc714b9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
