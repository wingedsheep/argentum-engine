package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Civic Wayfinder reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * RAV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CivicWayfinderReprint = Printing(
    oracleId = "b9e53414-c21f-4479-b8c1-8b36c3e6e006",
    name = "Civic Wayfinder",
    setCode = "10E",
    collectorNumber = "255",
    artist = "Cyril Van Der Haegen",
    imageUri = "https://cards.scryfall.io/normal/front/7/6/76d01445-ada9-452e-a6c0-d9dff59755f9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
