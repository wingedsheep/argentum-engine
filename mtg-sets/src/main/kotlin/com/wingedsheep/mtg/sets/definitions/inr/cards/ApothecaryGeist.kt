package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Apothecary Geist reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ApothecaryGeistReprint = Printing(
    oracleId = "3420da71-0af4-46b4-a994-91a82332d0fb",
    name = "Apothecary Geist",
    setCode = "INR",
    collectorNumber = "10",
    artist = "Franz Vohwinkel",
    imageUri = "https://cards.scryfall.io/normal/front/a/7/a79df1a1-6d60-4e5f-b4d8-668155836bb6.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
