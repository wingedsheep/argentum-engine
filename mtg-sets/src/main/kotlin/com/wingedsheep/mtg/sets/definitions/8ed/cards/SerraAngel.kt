package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Serra Angel reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SerraAngelReprint = Printing(
    oracleId = "4b7ac066-e5c7-43e6-9e7e-2739b24a905d",
    name = "Serra Angel",
    setCode = "8ED",
    collectorNumber = "45",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b089b409-2156-4c94-890f-e8804ab5a50c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
