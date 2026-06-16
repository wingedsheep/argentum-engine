package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Yavimaya Coast reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val YavimayaCoastReprint = Printing(
    oracleId = "40b36bc6-c185-4bda-99e7-0118953c2c97",
    name = "Yavimaya Coast",
    setCode = "10E",
    collectorNumber = "363",
    artist = "Anthony S. Waters",
    imageUri = "https://cards.scryfall.io/normal/front/7/d/7d1d58ea-39f2-4bdb-a960-b610c39e35e3.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
