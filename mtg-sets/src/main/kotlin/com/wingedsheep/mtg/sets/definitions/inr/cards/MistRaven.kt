package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mist Raven reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MistRavenReprint = Printing(
    oracleId = "12654457-3aae-4046-a976-8e41c3f78927",
    name = "Mist Raven",
    setCode = "INR",
    collectorNumber = "76",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/5/5/557b7e56-36f7-489c-a0fe-039c463f8bc5.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
