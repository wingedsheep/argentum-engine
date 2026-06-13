package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hill Giant reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HillGiantReprint = Printing(
    oracleId = "342199e0-15b6-4824-83da-25caef2592b3",
    name = "Hill Giant",
    setCode = "LEB",
    collectorNumber = "158",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/4/9/4905e98f-0c5a-4ec7-b85b-dc2c3549d5d0.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
