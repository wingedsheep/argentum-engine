package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Wraith reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BogWraithReprint = Printing(
    oracleId = "508248d1-09a4-4e41-a4c9-286618e5061e",
    name = "Bog Wraith",
    setCode = "8ED",
    collectorNumber = "120",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/0/1/013d65c4-f471-4e05-84fd-cb83393bee49.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
