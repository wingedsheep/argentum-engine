package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Righteousness reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RighteousnessReprint = Printing(
    oracleId = "3f6b2f76-0364-415e-a6a2-e9e5bf31b745",
    name = "Righteousness",
    setCode = "10E",
    collectorNumber = "36",
    artist = "Wayne England",
    imageUri = "https://cards.scryfall.io/normal/front/b/d/bda6c9d5-113f-44f1-bfaf-c40001ba9f60.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
