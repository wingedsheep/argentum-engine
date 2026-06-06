package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Glorious Anthem reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GloriousAnthemReprint = Printing(
    oracleId = "e3886fe8-9b76-4613-8891-4ec74657c087",
    name = "Glorious Anthem",
    setCode = "8ED",
    collectorNumber = "20",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/4/5/45f95890-0258-431c-b639-c0aba27b8039.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
