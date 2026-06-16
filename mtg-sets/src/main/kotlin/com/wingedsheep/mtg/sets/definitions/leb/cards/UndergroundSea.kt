package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Underground Sea reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UndergroundSeaReprint = Printing(
    oracleId = "4b22be3a-8ce1-47d1-b82e-6c3ccfb0548b",
    name = "Underground Sea",
    setCode = "LEB",
    collectorNumber = "286",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/5/e/5e91ce41-053e-4203-8860-49cbf854cc18.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
