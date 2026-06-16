package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Righteousness reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RighteousnessReprint = Printing(
    oracleId = "3f6b2f76-0364-415e-a6a2-e9e5bf31b745",
    name = "Righteousness",
    setCode = "LEB",
    collectorNumber = "37",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b847a2d1-5912-4f88-a68f-06790d0795dc.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
