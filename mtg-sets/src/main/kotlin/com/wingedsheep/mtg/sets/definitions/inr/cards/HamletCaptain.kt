package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hamlet Captain reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HamletCaptainReprint = Printing(
    oracleId = "1a60dc0f-e387-4d28-8ff0-c3f1883d0c5d",
    name = "Hamlet Captain",
    setCode = "INR",
    collectorNumber = "201",
    artist = "Wayne Reynolds",
    imageUri = "https://cards.scryfall.io/normal/front/0/6/06e5d76a-0cec-40e0-b694-2b5c8484f6c0.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
