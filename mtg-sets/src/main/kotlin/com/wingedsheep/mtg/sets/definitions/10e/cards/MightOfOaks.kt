package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Might of Oaks reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MightOfOaksReprint = Printing(
    oracleId = "8331f281-819b-4a0b-bad7-bd86dbedb877",
    name = "Might of Oaks",
    setCode = "10E",
    collectorNumber = "277",
    artist = "Jeremy Jarvis",
    imageUri = "https://cards.scryfall.io/normal/front/7/3/73957035-c02f-4de6-9258-ef339565bd1d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
