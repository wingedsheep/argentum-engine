package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin King reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinKingReprint = Printing(
    oracleId = "d236b3fc-0d3f-4d99-875d-e32a33fe5767",
    name = "Goblin King",
    setCode = "8ED",
    collectorNumber = "190",
    artist = "Ron Spears",
    imageUri = "https://cards.scryfall.io/normal/front/c/5/c5448159-0516-4f35-a212-c3e1efa84c71.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
