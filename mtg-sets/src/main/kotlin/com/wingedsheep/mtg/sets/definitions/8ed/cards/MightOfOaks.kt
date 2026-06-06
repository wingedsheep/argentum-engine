package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Might of Oaks reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MightOfOaksReprint = Printing(
    oracleId = "8331f281-819b-4a0b-bad7-bd86dbedb877",
    name = "Might of Oaks",
    setCode = "8ED",
    collectorNumber = "265",
    artist = "Greg Staples",
    imageUri = "https://cards.scryfall.io/normal/front/5/b/5b136d50-732b-4265-97c4-1caaa4a54be1.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
