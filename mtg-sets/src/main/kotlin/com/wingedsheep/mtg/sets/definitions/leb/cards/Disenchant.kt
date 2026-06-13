package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Disenchant reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DisenchantReprint = Printing(
    oracleId = "a7e97fa9-4b72-4548-b854-5be5f18a6f1a",
    name = "Disenchant",
    setCode = "LEB",
    collectorNumber = "19",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/9/d/9d61d0a5-7e92-4413-9121-925e1876b64d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
