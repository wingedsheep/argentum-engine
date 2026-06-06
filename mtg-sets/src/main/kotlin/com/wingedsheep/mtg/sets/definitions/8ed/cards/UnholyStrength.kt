package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unholy Strength reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UnholyStrengthReprint = Printing(
    oracleId = "090d88a9-7f2d-4bd1-a30a-7c48d05068be",
    name = "Unholy Strength",
    setCode = "8ED",
    collectorNumber = "169",
    artist = "Puddnhead",
    imageUri = "https://cards.scryfall.io/normal/front/7/d/7dd14a78-d306-43b1-b824-e04c96ce9155.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
