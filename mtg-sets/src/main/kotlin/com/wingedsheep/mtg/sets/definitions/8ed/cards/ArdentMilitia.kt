package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ardent Militia reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ArdentMilitiaReprint = Printing(
    oracleId = "23625877-b6db-480c-8885-a62b7d0457df",
    name = "Ardent Militia",
    setCode = "8ED",
    collectorNumber = "3",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/8/c/8c78d392-9f2f-4241-9a10-6ac9b1e86154.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
