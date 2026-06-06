package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Fisher reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvenFisherReprint = Printing(
    oracleId = "69a72414-d02f-4a5c-b5c9-8941bb181f61",
    name = "Aven Fisher",
    setCode = "8ED",
    collectorNumber = "61",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/a/d/adfc5265-7e38-4f2e-85ae-08f34eaffb89.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
