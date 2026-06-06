package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Lava Axe reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LavaAxeReprint = Printing(
    oracleId = "387b6b07-a283-412d-94c3-f7f1dc76e858",
    name = "Lava Axe",
    setCode = "8ED",
    collectorNumber = "197",
    artist = "Brian Snõddy",
    imageUri = "https://cards.scryfall.io/normal/front/4/d/4ddcf8a1-7337-41e9-b3b1-013f23b92eb0.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
