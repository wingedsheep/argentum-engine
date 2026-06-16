package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wretched Throng reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VOW's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WretchedThrongReprint = Printing(
    oracleId = "7e484372-a052-4df6-a6b2-2c6accccabef",
    name = "Wretched Throng",
    setCode = "INR",
    collectorNumber = "94",
    artist = "Filip Burburan",
    imageUri = "https://cards.scryfall.io/normal/front/a/8/a8ecbf3e-24ea-4bce-b97b-7f96668f2e14.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
