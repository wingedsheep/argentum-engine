package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrenn and Seven reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, loyalty) lives in
 * MID's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WrennAndSevenReprint = Printing(
    oracleId = "63a509ea-cedc-40e9-b4e8-0ff4fc356485",
    name = "Wrenn and Seven",
    setCode = "INR",
    collectorNumber = "226",
    artist = "Heonhwa",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/302f0dc1-88ab-4961-b78c-fbe7980dca18.jpg?1783908083",
    releaseDate = "2025-01-24",
    rarity = Rarity.MYTHIC,
)
