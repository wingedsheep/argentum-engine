package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rukh Egg reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ARN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RukhEggReprint = Printing(
    oracleId = "98116aec-2ab1-4bee-b727-9feff6274825",
    name = "Rukh Egg",
    setCode = "8ED",
    collectorNumber = "216",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d805f1f4-ac45-404d-b52c-4d5eb019e24a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
