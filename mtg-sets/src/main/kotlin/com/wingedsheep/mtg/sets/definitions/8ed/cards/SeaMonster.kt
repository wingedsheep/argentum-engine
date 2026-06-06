package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sea Monster reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SeaMonsterReprint = Printing(
    oracleId = "21b07ce7-b4f9-438c-8c09-e624557d62d2",
    name = "Sea Monster",
    setCode = "8ED",
    collectorNumber = "99",
    artist = "Daniel Gelon",
    imageUri = "https://cards.scryfall.io/normal/front/9/a/9a5094b8-085f-4a68-81bf-b1a7ab423ac4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
