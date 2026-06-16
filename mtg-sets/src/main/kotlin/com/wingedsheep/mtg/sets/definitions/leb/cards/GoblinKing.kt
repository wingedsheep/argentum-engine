package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin King reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinKingReprint = Printing(
    oracleId = "d236b3fc-0d3f-4d99-875d-e32a33fe5767",
    name = "Goblin King",
    setCode = "LEB",
    collectorNumber = "155",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/6/5/65705a8d-6bb1-4289-b8b0-8546ccc478dc.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
