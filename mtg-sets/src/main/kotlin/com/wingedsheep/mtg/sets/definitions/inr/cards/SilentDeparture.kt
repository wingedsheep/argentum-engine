package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Silent Departure reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SilentDepartureReprint = Printing(
    oracleId = "c41641f0-1abf-4776-9dec-1882f2d9badd",
    name = "Silent Departure",
    setCode = "INR",
    collectorNumber = "84",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/4/d/4d1552e7-20b2-42b0-af47-6618ab163115.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
