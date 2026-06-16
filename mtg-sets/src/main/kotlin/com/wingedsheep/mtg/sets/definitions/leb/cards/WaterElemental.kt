package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Water Elemental reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WaterElementalReprint = Printing(
    oracleId = "c470fbe1-0538-46ec-8741-3143f5e78af8",
    name = "Water Elemental",
    setCode = "LEB",
    collectorNumber = "92",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/6/6/66f729e2-565b-4cdb-8b6f-0a14babe5680.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
