package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Diving Griffin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DivingGriffinReprint = Printing(
    oracleId = "8785f9b4-eea2-4b45-9ae4-194b9a715702",
    name = "Diving Griffin",
    setCode = "8ED",
    collectorNumber = "17",
    artist = "John Howe",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bfeb0854-87e7-4236-9791-8fe601704200.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
