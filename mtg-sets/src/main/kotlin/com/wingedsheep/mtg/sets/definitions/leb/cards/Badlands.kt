package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Badlands reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BadlandsReprint = Printing(
    oracleId = "13ff3222-91cb-4796-a34e-899ed817694c",
    name = "Badlands",
    setCode = "LEB",
    collectorNumber = "278",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/a/3/a3393436-3426-4903-8f41-7abcbf6c18c2.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
