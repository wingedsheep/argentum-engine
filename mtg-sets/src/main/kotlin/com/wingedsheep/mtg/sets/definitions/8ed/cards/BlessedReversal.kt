package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Blessed Reversal reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BlessedReversalReprint = Printing(
    oracleId = "70e0b676-61a2-4dcf-8f61-d9281467ed43",
    name = "Blessed Reversal",
    setCode = "8ED",
    collectorNumber = "7",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/8/0/806d854b-a165-4d40-86d7-d6a0aa17ce18.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
