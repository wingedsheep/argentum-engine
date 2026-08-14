package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodmad Vampire reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BloodmadVampireReprint = Printing(
    oracleId = "c8a20fc7-025b-403a-b893-ff5efcbdc9d8",
    name = "Bloodmad Vampire",
    setCode = "INR",
    collectorNumber = "145",
    artist = "Johannes Voss",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8a9747c1-ef2c-4c61-9529-abfb6ae0f964.jpg?1783908123",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
