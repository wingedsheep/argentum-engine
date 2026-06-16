package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blue Elemental Blast reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlueElementalBlastReprint = Printing(
    oracleId = "65e1558c-6b09-4ddc-b520-f19f4fb972af",
    name = "Blue Elemental Blast",
    setCode = "LEB",
    collectorNumber = "50",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/7/f/7f07e272-6cc7-46d6-ad5c-473d1021c179.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
