package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Frozen Shade reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FrozenShadeReprint = Printing(
    oracleId = "f75f9006-217d-4ec8-9c35-ceefe1c5ae4e",
    name = "Frozen Shade",
    setCode = "LEB",
    collectorNumber = "110",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/8/9/89b6a352-40f5-4d7c-b2b6-2617539a1c1c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
