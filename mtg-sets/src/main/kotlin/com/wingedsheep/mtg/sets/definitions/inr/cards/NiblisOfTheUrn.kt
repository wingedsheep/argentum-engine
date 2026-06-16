package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Niblis of the Urn reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DKA's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NiblisOfTheUrnReprint = Printing(
    oracleId = "3ea7a555-d6fa-4cc9-9348-784e6f9a8cfc",
    name = "Niblis of the Urn",
    setCode = "INR",
    collectorNumber = "35",
    artist = "Ekaterina Burmak",
    imageUri = "https://cards.scryfall.io/normal/front/1/f/1ffd406d-92d3-47f1-8887-853958fd464e.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
