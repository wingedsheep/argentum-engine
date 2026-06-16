package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Valorous Stance reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * FRF's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ValorousStanceReprint = Printing(
    oracleId = "1f938353-9081-4dc1-b8e7-d18ece038bd4",
    name = "Valorous Stance",
    setCode = "INR",
    collectorNumber = "48",
    artist = "Anato Finnstark",
    imageUri = "https://cards.scryfall.io/normal/front/0/4/04762c85-d25d-4120-adec-8681adcf581a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
