package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Looming Shade reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LoomingShadeReprint = Printing(
    oracleId = "3ce8560d-dc68-4551-81c9-3a7b8d8da9a1",
    name = "Looming Shade",
    setCode = "8ED",
    collectorNumber = "140",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/5/3/53cf28d4-939c-495a-a6eb-e4f52a62e150.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
