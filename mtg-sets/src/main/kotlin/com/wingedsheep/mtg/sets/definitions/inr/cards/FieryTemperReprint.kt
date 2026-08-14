package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fiery Temper reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TOR's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FieryTemperReprint = Printing(
    oracleId = "f07bd49d-8e71-4d56-be2a-638514011318",
    name = "Fiery Temper",
    setCode = "INR",
    collectorNumber = "154",
    artist = "Johannes Voss",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d3a3a1d-c393-4a57-8a0e-5907d1722331.jpg?1783908123",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
