package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Think Twice reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThinkTwiceReprint = Printing(
    oracleId = "fa85c5a2-8e83-4624-a35a-a0bbf17ecbb4",
    name = "Think Twice",
    setCode = "INR",
    collectorNumber = "92",
    artist = "Anthony Francisco",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/35471afe-e14c-48f4-b901-297111be9c23.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
