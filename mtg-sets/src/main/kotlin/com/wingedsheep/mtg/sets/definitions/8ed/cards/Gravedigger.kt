package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gravedigger reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GravediggerReprint = Printing(
    oracleId = "1a2030cc-d7ee-4059-b2d7-fb95ea8e267b",
    name = "Gravedigger",
    setCode = "8ED",
    collectorNumber = "138",
    artist = "Dermot Power",
    imageUri = "https://cards.scryfall.io/normal/front/2/6/2679472e-39a5-4a28-a04e-962c393fbcc4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
