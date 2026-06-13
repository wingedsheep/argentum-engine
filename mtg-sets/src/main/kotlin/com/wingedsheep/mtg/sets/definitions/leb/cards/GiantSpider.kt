package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Spider reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GiantSpiderReprint = Printing(
    oracleId = "e740ce2f-2134-473c-afa1-1b6d2d1e38ef",
    name = "Giant Spider",
    setCode = "LEB",
    collectorNumber = "199",
    artist = "Sandra Everingham",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52ea35ce-8aa1-4818-8ad5-7e462452f10e.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
