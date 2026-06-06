package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Norwood Ranger reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NorwoodRangerReprint = Printing(
    oracleId = "e62ec170-c588-41ff-8be8-b05a125693a1",
    name = "Norwood Ranger",
    setCode = "8ED",
    collectorNumber = "271",
    artist = "Ron Spencer",
    imageUri = "https://cards.scryfall.io/normal/front/3/4/3415e3a7-99b2-44b5-93a3-fd07c4ecca4d.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
