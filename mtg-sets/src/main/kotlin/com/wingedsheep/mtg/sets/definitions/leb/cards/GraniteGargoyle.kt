package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Granite Gargoyle reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GraniteGargoyleReprint = Printing(
    oracleId = "ef5248d4-4cd1-447c-bdcc-78a09a67d923",
    name = "Granite Gargoyle",
    setCode = "LEB",
    collectorNumber = "156",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/a/f/affb57f4-273a-425c-a1b3-d0a5407f43d5.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
