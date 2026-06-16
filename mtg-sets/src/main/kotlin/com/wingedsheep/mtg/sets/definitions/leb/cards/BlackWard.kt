package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Black Ward reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlackWardReprint = Printing(
    oracleId = "7861ac9b-3024-4935-804c-2ca4c5a46bf4",
    name = "Black Ward",
    setCode = "LEB",
    collectorNumber = "5",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/3/0/30d5d3fe-5741-40f7-8f45-dadb818d79b0.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
