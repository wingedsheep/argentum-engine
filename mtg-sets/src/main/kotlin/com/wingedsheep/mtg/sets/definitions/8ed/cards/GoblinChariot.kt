package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Chariot reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * S99's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinChariotReprint = Printing(
    oracleId = "6ef0dd51-31df-4fd3-8153-49a427a7fdcf",
    name = "Goblin Chariot",
    setCode = "8ED",
    collectorNumber = "188",
    artist = "John Howe",
    imageUri = "https://cards.scryfall.io/normal/front/b/6/b6590436-d0f9-4c50-bab4-f7a094aa6e1a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
