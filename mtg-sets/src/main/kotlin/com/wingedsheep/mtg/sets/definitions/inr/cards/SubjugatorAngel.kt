package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Subjugator Angel reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SubjugatorAngelReprint = Printing(
    oracleId = "88b1e06c-5899-4e83-8204-e2c32c0c6aff",
    name = "Subjugator Angel",
    setCode = "INR",
    collectorNumber = "43",
    artist = "Lius Lasahido",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bb89585f-0c68-42e6-8115-07d5752f6c1c.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
