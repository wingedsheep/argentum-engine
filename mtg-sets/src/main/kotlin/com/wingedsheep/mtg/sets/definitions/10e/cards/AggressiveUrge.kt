package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aggressive Urge reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AggressiveUrgeReprint = Printing(
    oracleId = "43f5a93b-0f8d-48d2-ab9d-275d44cf88b5",
    name = "Aggressive Urge",
    setCode = "10E",
    collectorNumber = "250",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bf92ae01-9d1e-4b41-b068-096648daadf6.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
