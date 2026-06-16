package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodfire Colossus reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BloodfireColossusReprint = Printing(
    oracleId = "cc59e28a-eda3-468c-a31b-73e4b615f953",
    name = "Bloodfire Colossus",
    setCode = "10E",
    collectorNumber = "191",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/4/0/4050c0a3-a318-4283-ae72-8fffcf8d45e9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
