package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aim High reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AimHighReprint = Printing(
    oracleId = "c169929a-850b-4d46-9f14-a922f4465d85",
    name = "Aim High",
    setCode = "INR",
    collectorNumber = "185",
    artist = "Steve Prescott",
    imageUri = "https://cards.scryfall.io/normal/front/b/2/b28dbf2c-9bfe-42be-be55-1e7fb523df36.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
