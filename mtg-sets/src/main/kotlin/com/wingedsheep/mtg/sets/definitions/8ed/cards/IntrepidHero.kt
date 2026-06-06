package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Intrepid Hero reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IntrepidHeroReprint = Printing(
    oracleId = "63c1dfcc-9d47-4dee-b4e6-1f50e716cf9e",
    name = "Intrepid Hero",
    setCode = "8ED",
    collectorNumber = "26",
    artist = "Greg Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/5/9/592d08a6-730c-45a3-9d17-d724d2bdfbcd.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
