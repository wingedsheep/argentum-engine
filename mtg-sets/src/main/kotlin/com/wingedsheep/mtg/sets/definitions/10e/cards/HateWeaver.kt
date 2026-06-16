package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hate Weaver reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HateWeaverReprint = Printing(
    oracleId = "2a41f61b-a419-4b79-a25f-2506eaf05f55",
    name = "Hate Weaver",
    setCode = "10E",
    collectorNumber = "147",
    artist = "Roger Raupp",
    imageUri = "https://cards.scryfall.io/normal/front/b/5/b5b3c413-96ca-42c5-b263-f2da195c1854.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
