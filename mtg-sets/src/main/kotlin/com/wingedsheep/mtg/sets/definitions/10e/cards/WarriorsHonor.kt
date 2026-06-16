package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Warrior's Honor reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WarriorsHonorReprint = Printing(
    oracleId = "c3c9da49-ef4f-48c7-99c8-0f226b485285",
    name = "Warrior's Honor",
    setCode = "10E",
    collectorNumber = "58",
    artist = "D. Alexander Gregory",
    imageUri = "https://cards.scryfall.io/normal/front/0/9/09117d06-79e6-4f86-ba92-d1f8fe165147.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
