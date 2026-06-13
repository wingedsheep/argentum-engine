package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stream of Life reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StreamOfLifeReprint = Printing(
    oracleId = "9eb2912d-2130-49f2-9529-b58fa5a97a15",
    name = "Stream of Life",
    setCode = "LEB",
    collectorNumber = "218",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/d/a/da18a2c9-850e-400d-b0b3-edd8a946e380.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
