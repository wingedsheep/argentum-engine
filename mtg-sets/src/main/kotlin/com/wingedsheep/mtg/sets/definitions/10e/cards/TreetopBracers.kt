package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Treetop Bracers reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TreetopBracersReprint = Printing(
    oracleId = "ba71eb8a-f9f8-4608-88d8-963a1770d8d4",
    name = "Treetop Bracers",
    setCode = "10E",
    collectorNumber = "304",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b84185d8-c058-4fe7-b46e-f5ca90e70f12.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
