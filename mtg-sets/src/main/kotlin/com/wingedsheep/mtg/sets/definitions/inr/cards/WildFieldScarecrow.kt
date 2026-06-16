package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wild-Field Scarecrow reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WildFieldScarecrowReprint = Printing(
    oracleId = "f6a07af3-e54b-4dcb-ae2a-3dfdac386bab",
    name = "Wild-Field Scarecrow",
    setCode = "INR",
    collectorNumber = "274",
    artist = "Jakub Kasper",
    imageUri = "https://cards.scryfall.io/normal/front/1/3/13629238-f907-47b7-bdba-164e38c7b6d2.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
