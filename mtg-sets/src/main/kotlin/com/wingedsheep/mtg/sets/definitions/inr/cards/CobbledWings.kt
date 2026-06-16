package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cobbled Wings reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CobbledWingsReprint = Printing(
    oracleId = "8d8682f3-9ef3-4aa7-9ea6-8a2ce09bff6f",
    name = "Cobbled Wings",
    setCode = "INR",
    collectorNumber = "258",
    artist = "Matt Stewart",
    imageUri = "https://cards.scryfall.io/normal/front/e/8/e8bb16f6-71b2-4338-a546-27f37f50e811.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
