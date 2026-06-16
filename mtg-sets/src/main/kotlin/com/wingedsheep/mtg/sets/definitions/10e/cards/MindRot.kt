package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Rot reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MindRotReprint = Printing(
    oracleId = "ad44cf74-b717-48fb-9fa2-77512024d76a",
    name = "Mind Rot",
    setCode = "10E",
    collectorNumber = "159",
    artist = "Steve Luke",
    imageUri = "https://cards.scryfall.io/normal/front/e/7/e77cb309-1fb2-451e-a96d-b02b8c99932c.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
