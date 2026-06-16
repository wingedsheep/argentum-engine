package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Time Walk reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TimeWalkReprint = Printing(
    oracleId = "d0209d3f-3f7e-4fd5-bce5-10bce6f29c86",
    name = "Time Walk",
    setCode = "LEB",
    collectorNumber = "84",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/5/4/54992fda-45a9-4ed1-b380-34d167feec90.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
