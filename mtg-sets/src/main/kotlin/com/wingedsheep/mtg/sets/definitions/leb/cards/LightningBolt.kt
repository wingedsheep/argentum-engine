package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Bolt reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LightningBoltReprint = Printing(
    oracleId = "4457ed35-7c10-48c8-9776-456485fdf070",
    name = "Lightning Bolt",
    setCode = "LEB",
    collectorNumber = "162",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b5d3dcab-2260-479d-9ef6-dfb92d4f6061.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
