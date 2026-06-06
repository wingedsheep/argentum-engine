package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Glory Seeker reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GlorySeekerReprint = Printing(
    oracleId = "55c60b29-52c5-428c-9229-11ed0e26bd71",
    name = "Glory Seeker",
    setCode = "8ED",
    collectorNumber = "21",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/5/0/502beb90-c562-470a-9315-bc1a1b11ea9c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
