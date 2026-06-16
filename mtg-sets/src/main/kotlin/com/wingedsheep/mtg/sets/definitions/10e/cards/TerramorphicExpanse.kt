package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Terramorphic Expanse reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TerramorphicExpanseReprint = Printing(
    oracleId = "1bd3e453-aa21-4ee6-95c2-d6d920ee8e7a",
    name = "Terramorphic Expanse",
    setCode = "10E",
    collectorNumber = "360",
    artist = "Dan Murayama Scott",
    imageUri = "https://cards.scryfall.io/normal/front/6/8/6858542c-b8f9-449b-b7da-65c96a67636a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
