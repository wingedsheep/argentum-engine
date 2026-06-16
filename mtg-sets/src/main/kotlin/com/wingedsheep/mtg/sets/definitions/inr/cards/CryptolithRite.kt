package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cryptolith Rite reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CryptolithRiteReprint = Printing(
    oracleId = "043f869d-b11c-4c0d-9591-2bf0df7bde55",
    name = "Cryptolith Rite",
    setCode = "INR",
    collectorNumber = "189",
    artist = "Zack Stella",
    imageUri = "https://cards.scryfall.io/normal/front/0/a/0a4e707f-b760-434d-ba97-a0fe651f3f6a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)
