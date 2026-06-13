package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Uthden Troll reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UthdenTrollReprint = Printing(
    oracleId = "d6329dcc-b450-482a-8ee3-45449f7a4b3d",
    name = "Uthden Troll",
    setCode = "LEB",
    collectorNumber = "181",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/9/1/91f46e9a-6075-4fa5-8f60-f81e2024b13d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
