package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sword Dancer reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SwordDancerReprint = Printing(
    oracleId = "9ae49b45-a38f-4bd6-aa08-b5a5da6e75a1",
    name = "Sword Dancer",
    setCode = "8ED",
    collectorNumber = "53",
    artist = "Roger Raupp",
    imageUri = "https://cards.scryfall.io/normal/front/e/c/ec270d53-042e-4b11-b0f4-6416348b8fff.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
