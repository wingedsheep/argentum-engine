package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fear reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FearReprint = Printing(
    oracleId = "355bbe9b-59bf-470f-8600-410af4c7fe18",
    name = "Fear",
    setCode = "10E",
    collectorNumber = "142",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/d/8/d8fa57eb-e307-4104-a291-c6cbfa235816.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
