package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Deluge reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DelugeReprint = Printing(
    oracleId = "cd271e61-0135-484f-ae02-5aaca57c1124",
    name = "Deluge",
    setCode = "10E",
    collectorNumber = "79",
    artist = "Wayne England",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/baf37153-ec4c-4c2f-89f7-450f0a157311.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
