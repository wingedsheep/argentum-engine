package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Savannah reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SavannahReprint = Printing(
    oracleId = "703243f0-8cb3-420f-958f-5fd4bde30293",
    name = "Savannah",
    setCode = "LEB",
    collectorNumber = "281",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0e9aeaa8-9a75-4719-992f-cbb316f72175.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
