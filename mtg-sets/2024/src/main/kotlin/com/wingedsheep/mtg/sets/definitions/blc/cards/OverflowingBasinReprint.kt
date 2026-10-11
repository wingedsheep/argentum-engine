package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Overflowing Basin reprint in BLC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * another set's `cards/` package. This file contributes only the BLC-specific
 * presentation row — set, collector number, art — picked up automatically by
 * `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val OverflowingBasinReprint = Printing(
    oracleId = "5ac8e01c-b0a7-4855-a122-1cd26b07c4a5",
    name = "Overflowing Basin",
    setCode = "BLC",
    collectorNumber = "321",
    scryfallId = "c41cc911-544f-43b7-8a86-8a369e3069b8",
    artist = "Sarah Finnigan",
    imageUri = "https://cards.scryfall.io/normal/front/c/4/c41cc911-544f-43b7-8a86-8a369e3069b8.jpg?1783910635",
    releaseDate = "2024-08-02",
    rarity = Rarity.RARE,
)
