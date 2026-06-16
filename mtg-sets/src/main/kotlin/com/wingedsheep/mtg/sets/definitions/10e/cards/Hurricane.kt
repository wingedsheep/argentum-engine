package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hurricane reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HurricaneReprint = Printing(
    oracleId = "9c021685-4017-49c7-9f58-2ae0243361a0",
    name = "Hurricane",
    setCode = "10E",
    collectorNumber = "270",
    artist = "John Howe",
    imageUri = "https://cards.scryfall.io/normal/front/3/7/371d83e8-f514-433c-bc6a-e0eeef3fab2a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
