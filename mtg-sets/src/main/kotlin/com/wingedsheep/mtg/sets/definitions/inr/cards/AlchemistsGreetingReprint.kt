package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Alchemist's Greeting reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AlchemistsGreetingReprint = Printing(
    oracleId = "9aaf0df7-5e2d-4f49-a8cb-66523be15ad6",
    name = "Alchemist's Greeting",
    setCode = "INR",
    collectorNumber = "140",
    artist = "Jakub Kasper",
    imageUri = "https://cards.scryfall.io/normal/front/f/3/f36e2146-b6a9-4b61-9ccf-969a2c79b747.jpg?1783908129",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
