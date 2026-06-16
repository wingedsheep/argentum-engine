package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mesa Pegasus reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MesaPegasusReprint = Printing(
    oracleId = "8161f5b8-6aab-4133-ba2c-2e7b5774153e",
    name = "Mesa Pegasus",
    setCode = "LEB",
    collectorNumber = "29",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/5/5/55bff46a-6725-4918-9bdf-38efaaf50236.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
