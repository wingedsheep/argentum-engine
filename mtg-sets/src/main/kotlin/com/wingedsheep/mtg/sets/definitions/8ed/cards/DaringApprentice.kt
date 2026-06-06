package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Daring Apprentice reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DaringApprenticeReprint = Printing(
    oracleId = "863ad4b8-094b-46b3-b6cd-0640e1f01d6d",
    name = "Daring Apprentice",
    setCode = "8ED",
    collectorNumber = "73",
    artist = "Dany Orizio",
    imageUri = "https://cards.scryfall.io/normal/front/f/b/fbbacff5-c650-4835-b5a1-f747a5db5fd6.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
