package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin King reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinKingReprint = Printing(
    oracleId = "d236b3fc-0d3f-4d99-875d-e32a33fe5767",
    name = "Goblin King",
    setCode = "10E",
    collectorNumber = "207",
    artist = "Ron Spears",
    imageUri = "https://cards.scryfall.io/normal/front/4/b/4b266935-25ea-49c5-a1da-57c22a4362fd.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
