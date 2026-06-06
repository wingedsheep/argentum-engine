package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Suntail Hawk reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * JUD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SuntailHawkReprint = Printing(
    oracleId = "a8d31e2f-7b2e-4135-8074-9e6ef778bd80",
    name = "Suntail Hawk",
    setCode = "8ED",
    collectorNumber = "51",
    artist = "Heather Hudson",
    imageUri = "https://cards.scryfall.io/normal/front/6/9/69414b6a-421e-4895-9262-d3097078fb12.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
