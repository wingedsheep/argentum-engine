package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Gravedigger reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GravediggerReprint = Printing(
    oracleId = "1a2030cc-d7ee-4059-b2d7-fb95ea8e267b",
    name = "Gravedigger",
    setCode = "10E",
    collectorNumber = "146",
    artist = "Dermot Power",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da57d35a-ca0b-4f93-867c-d0a9fb5108f1.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
