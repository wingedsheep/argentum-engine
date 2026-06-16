package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dragon Roost reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DragonRoostReprint = Printing(
    oracleId = "6dc98143-7c4c-4b75-9bbb-5226d800b1d6",
    name = "Dragon Roost",
    setCode = "10E",
    collectorNumber = "197",
    artist = "Jim Pavelec",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1da84a3e-a8b2-4cb4-b9c9-1b5c2dca8c67.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
