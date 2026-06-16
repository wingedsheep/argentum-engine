package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Grizzly Bears reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GrizzlyBearsReprint = Printing(
    oracleId = "14c8f55d-d177-4c25-a931-ebeb9e6062a0",
    name = "Grizzly Bears",
    setCode = "LEB",
    collectorNumber = "200",
    artist = "Jeff A. Menges",
    imageUri = "https://cards.scryfall.io/normal/front/e/7/e7aa2b93-0a84-4318-bf2d-58164f0a846f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
