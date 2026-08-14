package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Murderous Compulsion reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SOI's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MurderousCompulsionReprint = Printing(
    oracleId = "8fd8b4ef-69c8-43aa-9cbe-c46dba845ba8",
    name = "Murderous Compulsion",
    setCode = "INR",
    collectorNumber = "126",
    artist = "David Palumbo",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5fca1faf-bd47-45cd-a2d9-b2efbc75cbb5.jpg?1783908138",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
