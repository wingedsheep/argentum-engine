package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Cockroach reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantCockroachReprint = Printing(
    oracleId = "c8b49b28-d364-4cf0-a2a5-56f6f7dc1b22",
    name = "Giant Cockroach",
    setCode = "8ED",
    collectorNumber = "135",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/6/8/68b79ee0-0a1b-474c-b68c-6e4ec69c018c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
