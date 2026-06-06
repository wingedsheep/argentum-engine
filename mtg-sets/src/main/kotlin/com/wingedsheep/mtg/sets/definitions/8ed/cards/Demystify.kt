package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Demystify reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DemystifyReprint = Printing(
    oracleId = "fd591199-9f7a-4147-a150-13279dbb4498",
    name = "Demystify",
    setCode = "8ED",
    collectorNumber = "16",
    artist = "Christopher Rush",
    imageUri = "https://cards.scryfall.io/normal/front/4/9/4907c728-f08d-4c14-96a2-c92f09dfa6d2.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
