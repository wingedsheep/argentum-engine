package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Avacynian Priest reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvacynianPriestReprint = Printing(
    oracleId = "df7e296d-96de-4280-a4d8-49189544b9e3",
    name = "Avacynian Priest",
    setCode = "INR",
    collectorNumber = "12",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/c/1/c119b314-c563-4b6b-8bac-a680e69c6b37.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
