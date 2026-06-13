package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shivan Dragon reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShivanDragonReprint = Printing(
    oracleId = "711eea87-0fa3-46e0-a42b-fa5a86455f04",
    name = "Shivan Dragon",
    setCode = "LEB",
    collectorNumber = "175",
    artist = "Melissa A. Benson",
    imageUri = "https://cards.scryfall.io/normal/front/5/e/5e64822a-6817-4e1e-8155-3e95f8e3763f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
