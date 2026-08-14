package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tamiyo, Field Researcher reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, loyalty) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TamiyoFieldResearcherReprint = Printing(
    oracleId = "9cf99129-f51a-4418-a8cc-8ca2992b17fa",
    name = "Tamiyo, Field Researcher",
    setCode = "INR",
    collectorNumber = "249",
    artist = "Kieran Yanner",
    imageUri = "https://cards.scryfall.io/normal/front/5/7/576ba8db-fad9-4c48-96be-a8a7e5f43039.jpg?1783908070",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)
