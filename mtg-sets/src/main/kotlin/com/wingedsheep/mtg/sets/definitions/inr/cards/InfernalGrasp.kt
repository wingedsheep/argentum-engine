package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Infernal Grasp reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val InfernalGraspReprint = Printing(
    oracleId = "94f0a572-e91c-4b56-a5d1-6cbbeabd210d",
    name = "Infernal Grasp",
    setCode = "INR",
    collectorNumber = "119",
    artist = "Naomi Baker",
    imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b74630-1768-4b02-b7ca-37cd35ede6cf.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
