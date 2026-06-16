package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Peek reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PeekReprint = Printing(
    oracleId = "873abbf4-c9b5-4b8f-8cd0-613cf3b9b1d5",
    name = "Peek",
    setCode = "10E",
    collectorNumber = "94",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/8/6/869a6c06-eae5-418e-9a5c-26598a929416.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
