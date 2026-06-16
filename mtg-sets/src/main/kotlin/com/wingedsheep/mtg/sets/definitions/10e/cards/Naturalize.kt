package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Naturalize reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NaturalizeReprint = Printing(
    oracleId = "bdb3ca68-ec1f-4e16-81cc-d23f8f52c728",
    name = "Naturalize",
    setCode = "10E",
    collectorNumber = "282",
    artist = "Tim Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/8/b/8ba4e513-a8a7-4338-99de-b9303813d086.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
