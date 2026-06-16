package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Olivia's Dragoon reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OliviasDragoonReprint = Printing(
    oracleId = "a5019399-91a8-4233-b16f-399718c4be9c",
    name = "Olivia's Dragoon",
    setCode = "INR",
    collectorNumber = "127",
    artist = "Chris Rallis",
    imageUri = "https://cards.scryfall.io/normal/front/5/7/571a9e92-6f34-4bf4-b7f4-15d12560efb6.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
