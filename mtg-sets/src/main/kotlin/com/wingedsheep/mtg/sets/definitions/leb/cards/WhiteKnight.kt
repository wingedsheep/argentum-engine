package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * White Knight reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WhiteKnightReprint = Printing(
    oracleId = "ddb021df-ae4a-4ac1-8353-d0b375761714",
    name = "White Knight",
    setCode = "LEB",
    collectorNumber = "44",
    artist = "Daniel Gelon",
    imageUri = "https://cards.scryfall.io/normal/front/a/2/a231e0b8-b3e3-4f4a-8baa-c56626b01685.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
