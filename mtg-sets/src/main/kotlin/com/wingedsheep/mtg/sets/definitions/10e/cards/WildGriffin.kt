package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wild Griffin reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WildGriffinReprint = Printing(
    oracleId = "c643cfe1-5844-4eb1-b1f5-028382411773",
    name = "Wild Griffin",
    setCode = "10E",
    collectorNumber = "59",
    artist = "Matt Cavotta",
    imageUri = "https://cards.scryfall.io/normal/front/0/9/099ecf12-b750-4a95-85c8-8612d2a77a0a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
