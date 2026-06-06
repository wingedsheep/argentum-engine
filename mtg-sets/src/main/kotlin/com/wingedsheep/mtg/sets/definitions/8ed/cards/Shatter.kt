package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shatter reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShatterReprint = Printing(
    oracleId = "84d32e68-72b9-432d-a023-7bc6a26b8ad0",
    name = "Shatter",
    setCode = "8ED",
    collectorNumber = "220",
    artist = "Jason Alexander Behnke",
    imageUri = "https://cards.scryfall.io/normal/front/8/1/818401a7-593d-4f04-a782-9fa486b8c2f6.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
