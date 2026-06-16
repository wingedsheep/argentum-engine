package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lingering Souls reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DKA's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LingeringSoulsReprint = Printing(
    oracleId = "0b8c3337-04dd-4798-8203-6d8b8cfb936b",
    name = "Lingering Souls",
    setCode = "INR",
    collectorNumber = "30",
    artist = "Johann Bodin",
    imageUri = "https://cards.scryfall.io/normal/front/0/9/09cd56b2-1987-40c2-a436-5f3cbc564f1c.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
