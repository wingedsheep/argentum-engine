package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Wraith reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BogWraithReprint = Printing(
    oracleId = "508248d1-09a4-4e41-a4c9-286618e5061e",
    name = "Bog Wraith",
    setCode = "10E",
    collectorNumber = "130",
    artist = "Daarken",
    imageUri = "https://cards.scryfall.io/normal/front/d/1/d1b39f2d-96f2-4cac-9f6d-a061dbef749b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
