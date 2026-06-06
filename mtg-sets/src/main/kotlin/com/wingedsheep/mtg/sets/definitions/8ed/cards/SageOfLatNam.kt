package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sage of Lat-Nam reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ATQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SageOfLatNamReprint = Printing(
    oracleId = "f34be3cc-ff47-4415-a6a8-ed142891dc0c",
    name = "Sage of Lat-Nam",
    setCode = "8ED",
    collectorNumber = "97",
    artist = "Alan Pollack",
    imageUri = "https://cards.scryfall.io/normal/front/2/8/28ac8823-9bc0-4d15-a7ab-0d106167a0e5.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
