package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Web reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WebReprint = Printing(
    oracleId = "5aa12aff-db3c-4be5-822b-3afdf536b33e",
    name = "Web",
    setCode = "LEB",
    collectorNumber = "229",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/f/7/f7f84dc2-5a29-447d-97ab-a10afd9ee538.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
