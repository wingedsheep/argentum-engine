package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wild Hunger reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DKA's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WildHungerReprint = Printing(
    oracleId = "9d250088-d5f2-40ed-bdb7-a6decce0fb67",
    name = "Wild Hunger",
    setCode = "INR",
    collectorNumber = "225",
    artist = "Karl Kopinski",
    imageUri = "https://cards.scryfall.io/normal/front/5/d/5d42861d-4dff-41c6-83bb-d599230f7ed1.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
