package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Chittering Host reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EMN's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ChitteringHostReprint = Printing(
    oracleId = "6923cf6b-7d3e-4d95-abaf-1df1a04ac7c1",
    name = "Chittering Host",
    setCode = "INR",
    collectorNumber = "123b",
    artist = "Jason Felix",
    imageUri = "https://cards.scryfall.io/normal/front/6/0/607d82bd-13b1-499a-9005-0381e716013a.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
