package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Forbidden Alchemy reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ISD's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ForbiddenAlchemyReprint = Printing(
    oracleId = "66d65cda-e3d1-4a00-8ff3-c6d5ef0ab0c1",
    name = "Forbidden Alchemy",
    setCode = "INR",
    collectorNumber = "65",
    artist = "David Rapoza",
    imageUri = "https://cards.scryfall.io/normal/front/c/1/c179a72e-2956-43d3-810c-7eeca8d3ab0d.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.UNCOMMON,
)
