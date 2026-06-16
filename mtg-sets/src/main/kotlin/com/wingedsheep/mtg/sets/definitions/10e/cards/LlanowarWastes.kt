package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Llanowar Wastes reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LlanowarWastesReprint = Printing(
    oracleId = "32116127-cf96-4a1b-8896-a1ebc087b597",
    name = "Llanowar Wastes",
    setCode = "10E",
    collectorNumber = "355",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/f/a/fa9d9e2d-6176-4593-b34a-c892a7b34695.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
