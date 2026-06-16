package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Siege-Gang Commander reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SCG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SiegeGangCommanderReprint = Printing(
    oracleId = "ddc7f59a-bbb1-4ba1-82c8-6813fd191940",
    name = "Siege-Gang Commander",
    setCode = "10E",
    collectorNumber = "234",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/9/b/9b8a232d-7289-48c2-9160-54c1c4154721.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
