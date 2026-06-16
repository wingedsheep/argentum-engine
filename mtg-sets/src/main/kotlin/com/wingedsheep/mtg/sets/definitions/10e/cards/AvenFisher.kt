package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Fisher reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvenFisherReprint = Printing(
    oracleId = "69a72414-d02f-4a5c-b5c9-8941bb181f61",
    name = "Aven Fisher",
    setCode = "10E",
    collectorNumber = "68",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bba2cd91-84a7-4d05-9a0c-55440491be1e.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
