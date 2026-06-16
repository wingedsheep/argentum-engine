package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Traveler's Amulet reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TravelersAmuletReprint = Printing(
    oracleId = "48b561dd-f23d-409a-87c8-e49902b2477b",
    name = "Traveler's Amulet",
    setCode = "INR",
    collectorNumber = "273",
    artist = "Alan Pollack",
    imageUri = "https://cards.scryfall.io/normal/front/2/6/26a7e5f0-0d60-4118-ad10-b27c7382511a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
