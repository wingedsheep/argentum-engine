package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ridgeline Rager reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RidgelineRagerReprint = Printing(
    oracleId = "54e7a6d4-d405-4627-93ab-8d8bd945fe02",
    name = "Ridgeline Rager",
    setCode = "8ED",
    collectorNumber = "215",
    artist = "Chippy",
    imageUri = "https://cards.scryfall.io/normal/front/4/3/43d9c248-2360-4fdc-9a0f-49d350c11e95.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
