package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Earthquake reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EarthquakeReprint = Printing(
    oracleId = "9a40614b-50a3-422c-849e-53c8b7d3d204",
    name = "Earthquake",
    setCode = "LEB",
    collectorNumber = "147",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/8/6/86435875-ac92-4348-b41e-19570cf62a1c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
