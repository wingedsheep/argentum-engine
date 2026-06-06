package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wrath of God reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WrathOfGodReprint = Printing(
    oracleId = "34515b16-c9a4-4f98-8c77-416a7a523407",
    name = "Wrath of God",
    setCode = "8ED",
    collectorNumber = "58",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/0/6/0619d670-7b53-4185-a25d-2fab5db1aab5.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
