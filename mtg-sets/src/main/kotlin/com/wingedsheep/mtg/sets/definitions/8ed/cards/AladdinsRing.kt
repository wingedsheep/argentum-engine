package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aladdin's Ring reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ARN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AladdinsRingReprint = Printing(
    oracleId = "7810915f-df4d-445a-8ba8-fe6ed3abf1ae",
    name = "Aladdin's Ring",
    setCode = "8ED",
    collectorNumber = "291",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/6/f/6ffd2437-04e7-44e4-becc-15be62735b42.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
