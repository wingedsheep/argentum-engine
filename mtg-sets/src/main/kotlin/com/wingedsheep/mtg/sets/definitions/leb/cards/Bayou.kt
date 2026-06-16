package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bayou reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BayouReprint = Printing(
    oracleId = "b76d1ae6-ad1d-4bac-b4c3-2e03e0e84d9b",
    name = "Bayou",
    setCode = "LEB",
    collectorNumber = "279",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/1/7/17db2b6a-eaa8-4a08-9e86-370bbd058574.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
