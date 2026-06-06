package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Razorfoot Griffin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RazorfootGriffinReprint = Printing(
    oracleId = "c7ecb785-2bfc-4953-b356-db7c27406dbb",
    name = "Razorfoot Griffin",
    setCode = "8ED",
    collectorNumber = "36",
    artist = "Ben Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/6/0/6023b3c7-53bc-4490-8f9b-beb858b068e8.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
