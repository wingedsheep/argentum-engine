package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Reckless Scholar reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ZEN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RecklessScholarReprint = Printing(
    oracleId = "604aded2-64c2-4474-b5b7-8c83e0e3fe76",
    name = "Reckless Scholar",
    setCode = "INR",
    collectorNumber = "81",
    artist = "Steve Prescott",
    imageUri = "https://cards.scryfall.io/normal/front/c/e/ceb85311-a7f7-44d5-9caa-50a24fd6d00a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
