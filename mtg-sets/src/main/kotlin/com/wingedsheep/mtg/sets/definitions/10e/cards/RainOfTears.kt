package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rain of Tears reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RainOfTearsReprint = Printing(
    oracleId = "72cecab3-519e-4a23-9623-b423a5c5a251",
    name = "Rain of Tears",
    setCode = "10E",
    collectorNumber = "170",
    artist = "Eric Peterson",
    imageUri = "https://cards.scryfall.io/normal/front/5/8/5811f4ee-f352-4b41-8f56-da0cb7f3f11b.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
