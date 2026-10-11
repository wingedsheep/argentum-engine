package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sunscorched Divide reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * another set's `cards/` package. This file contributes only the BLC-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SunscorchedDivideReprint = Printing(
    oracleId = "8d2b2675-19df-4f40-9e8e-196ec097b91c",
    name = "Sunscorched Divide",
    setCode = "BLC",
    collectorNumber = "336",
    scryfallId = "2bc3a2d6-986e-4b66-a68c-847174657f36",
    artist = "Sarah Finnigan",
    imageUri = "https://cards.scryfall.io/normal/front/2/b/2bc3a2d6-986e-4b66-a68c-847174657f36.jpg?1783910629",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)
