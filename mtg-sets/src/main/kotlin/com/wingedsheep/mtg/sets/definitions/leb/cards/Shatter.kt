package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shatter reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShatterReprint = Printing(
    oracleId = "84d32e68-72b9-432d-a023-7bc6a26b8ad0",
    name = "Shatter",
    setCode = "LEB",
    collectorNumber = "174",
    artist = "Amy Weber",
    imageUri = "https://cards.scryfall.io/normal/front/7/6/76ddf3f4-1305-4599-bf4c-f9e148bdda4d.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
