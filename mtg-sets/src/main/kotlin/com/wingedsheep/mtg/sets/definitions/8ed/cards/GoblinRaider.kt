package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Raider reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinRaiderReprint = Printing(
    oracleId = "7f11c830-fced-40db-8c02-b3c54f3e4372",
    name = "Goblin Raider",
    setCode = "8ED",
    collectorNumber = "191",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/0/8/0837dd6a-d413-40a4-a684-ec637a4f3d4a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
