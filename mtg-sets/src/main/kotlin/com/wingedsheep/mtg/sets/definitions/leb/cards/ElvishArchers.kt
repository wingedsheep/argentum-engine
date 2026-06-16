package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Archers reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ElvishArchersReprint = Printing(
    oracleId = "534ce8e2-53a1-407c-9881-d3b5347d43c3",
    name = "Elvish Archers",
    setCode = "LEB",
    collectorNumber = "192",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/c/3/c3240d5e-b3d4-4368-b09b-c309bc935152.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
