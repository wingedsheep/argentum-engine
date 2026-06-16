package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Anaba Bodyguard reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * HML's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AnabaBodyguardReprint = Printing(
    oracleId = "8c64db66-879e-46ad-9d11-8a95b6cf5d19",
    name = "Anaba Bodyguard",
    setCode = "10E",
    collectorNumber = "187",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6fdd01bd-ab41-4005-8807-46db0cfc4da4.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
