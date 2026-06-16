package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mox Sapphire reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MoxSapphireReprint = Printing(
    oracleId = "d5ed1233-df87-4b90-8918-13922ec95249",
    name = "Mox Sapphire",
    setCode = "LEB",
    collectorNumber = "266",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/1/e/1eb3178b-dac5-4b34-9d3e-4f5a170d1c87.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
