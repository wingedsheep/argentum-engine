package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Guardian of Pilgrims reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GuardianOfPilgrimsReprint = Printing(
    oracleId = "a090bc9c-d253-4a29-87d6-aa849f856d36",
    name = "Guardian of Pilgrims",
    setCode = "INR",
    collectorNumber = "26",
    artist = "Lordigan",
    imageUri = "https://cards.scryfall.io/normal/front/f/8/f87b5e18-d1a9-424b-bec5-6a80f7699600.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
