package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Execute reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ExecuteReprint = Printing(
    oracleId = "24d2cbf6-6268-4da3-8413-6c76679576fe",
    name = "Execute",
    setCode = "8ED",
    collectorNumber = "132",
    artist = "Gary Ruddell",
    imageUri = "https://cards.scryfall.io/normal/front/e/e/ee0ecbc8-767b-475c-bb74-2abc0da5d745.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
