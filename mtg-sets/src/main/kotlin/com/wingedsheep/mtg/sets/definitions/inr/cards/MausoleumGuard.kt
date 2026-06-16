package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mausoleum Guard reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MausoleumGuardReprint = Printing(
    oracleId = "64f3af46-f8ab-4ba4-9dff-7412cefb4eea",
    name = "Mausoleum Guard",
    setCode = "INR",
    collectorNumber = "33",
    artist = "David Palumbo",
    imageUri = "https://cards.scryfall.io/normal/front/8/4/849bea7e-74e5-4310-be5a-d517d7b19be6.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
