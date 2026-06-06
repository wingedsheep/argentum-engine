package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hill Giant reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HillGiantReprint = Printing(
    oracleId = "342199e0-15b6-4824-83da-25caef2592b3",
    name = "Hill Giant",
    setCode = "8ED",
    collectorNumber = "194",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/f/3/f3afb143-fd69-4197-96f4-62d5d90a894c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
