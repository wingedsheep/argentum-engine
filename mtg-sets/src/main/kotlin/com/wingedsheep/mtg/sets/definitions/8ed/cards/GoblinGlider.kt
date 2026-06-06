package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Glider reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinGliderReprint = Printing(
    oracleId = "72bce821-46f0-40eb-abdb-575d4305b8fd",
    name = "Goblin Glider",
    setCode = "8ED",
    collectorNumber = "189",
    artist = "Patrick Faricy",
    imageUri = "https://cards.scryfall.io/normal/front/8/2/82db30ff-5f50-41b1-acdf-0a0e1a364410.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
