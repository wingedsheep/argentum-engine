package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spitting Earth reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpittingEarthReprint = Printing(
    oracleId = "0c3bf4e1-d91e-4dd3-a800-e40971222c71",
    name = "Spitting Earth",
    setCode = "10E",
    collectorNumber = "238",
    artist = "Michael Koelsch",
    imageUri = "https://cards.scryfall.io/normal/front/4/7/475294e2-792c-400b-b5ff-d4bf9e70718e.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
