package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Holy Strength reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HolyStrengthReprint = Printing(
    oracleId = "9357de36-f8be-4f49-b2c8-9fe9eaf82b07",
    name = "Holy Strength",
    setCode = "8ED",
    collectorNumber = "24",
    artist = "Scott M. Fischer",
    imageUri = "https://cards.scryfall.io/normal/front/b/0/b0038c0e-071f-4fee-bf23-d50d00014703.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
