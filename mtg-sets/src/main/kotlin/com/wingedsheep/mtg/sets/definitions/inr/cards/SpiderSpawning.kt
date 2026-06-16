package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider Spawning reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpiderSpawningReprint = Printing(
    oracleId = "da113cc9-bb2b-4af4-9c43-32c165b87363",
    name = "Spider Spawning",
    setCode = "INR",
    collectorNumber = "216",
    artist = "Daniel Ljunggren",
    imageUri = "https://cards.scryfall.io/normal/front/c/2/c229cbde-f370-4fef-8765-9773d516324a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
