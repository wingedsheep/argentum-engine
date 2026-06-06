package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Index reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val IndexReprint = Printing(
    oracleId = "613b824e-39d1-4c6e-a19c-c915abdc860b",
    name = "Index",
    setCode = "8ED",
    collectorNumber = "84",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/d/6/d64e0a7c-54a9-45b9-be46-ddf6755f1142.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
