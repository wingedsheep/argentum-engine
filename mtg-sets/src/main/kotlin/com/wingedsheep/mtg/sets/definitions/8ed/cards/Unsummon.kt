package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unsummon reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UnsummonReprint = Printing(
    oracleId = "837182db-1bf3-4a2c-bd01-1af9d9873561",
    name = "Unsummon",
    setCode = "8ED",
    collectorNumber = "112",
    artist = "Ron Spencer",
    imageUri = "https://cards.scryfall.io/normal/front/2/c/2cb14889-2992-4d99-a6fe-1e245475c758.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
