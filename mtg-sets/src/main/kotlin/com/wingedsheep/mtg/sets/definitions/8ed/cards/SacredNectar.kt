package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sacred Nectar reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SacredNectarReprint = Printing(
    oracleId = "30870ee5-6ad7-48a9-983e-d3b018f2344f",
    name = "Sacred Nectar",
    setCode = "8ED",
    collectorNumber = "40",
    artist = "Janine Johnston",
    imageUri = "https://cards.scryfall.io/normal/front/7/4/7417bbe7-c1b3-4046-98b3-7d6af9851ab2.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
