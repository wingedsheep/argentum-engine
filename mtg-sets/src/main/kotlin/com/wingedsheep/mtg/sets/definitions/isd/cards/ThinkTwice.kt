package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Think Twice reprint in ISD.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the ISD-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ThinkTwiceReprint = Printing(
    oracleId = "fa85c5a2-8e83-4624-a35a-a0bbf17ecbb4",
    name = "Think Twice",
    setCode = "ISD",
    collectorNumber = "83",
    artist = "Anthony Francisco",
    imageUri = "https://cards.scryfall.io/normal/front/5/3/53e44060-a9a2-4095-9f5b-f60297525315.jpg",
    releaseDate = "2011-09-30",
    rarity = Rarity.COMMON,
)
