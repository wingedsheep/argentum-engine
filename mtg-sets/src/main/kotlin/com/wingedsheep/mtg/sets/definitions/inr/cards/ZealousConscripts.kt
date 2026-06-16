package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Zealous Conscripts reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * AVR's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ZealousConscriptsReprint = Printing(
    oracleId = "1dae6f39-2cbd-485c-b190-017a26401fd4",
    name = "Zealous Conscripts",
    setCode = "INR",
    collectorNumber = "183",
    artist = "Steve Prescott",
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b5ca6c08-bfe0-4021-b6ad-e235c8905661.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.RARE,
)
