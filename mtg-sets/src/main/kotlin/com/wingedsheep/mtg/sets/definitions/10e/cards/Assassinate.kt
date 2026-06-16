package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Assassinate reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AssassinateReprint = Printing(
    oracleId = "ce979f36-84f2-4419-b400-971263494cc6",
    name = "Assassinate",
    setCode = "10E",
    collectorNumber = "128",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/d/7/d79599f4-7038-4b8f-9a44-e58b650f21a7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
