package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Terror reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TerrorReprint = Printing(
    oracleId = "b81f041d-98db-4408-9472-c483e4a502bc",
    name = "Terror",
    setCode = "10E",
    collectorNumber = "182",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/3/d/3d1ccc3b-a6bd-4dc8-b7ba-99172d612106.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
