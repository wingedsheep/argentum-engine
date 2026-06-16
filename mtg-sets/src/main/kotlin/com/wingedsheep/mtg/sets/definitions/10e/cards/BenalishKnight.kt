package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Benalish Knight reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * WTH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BenalishKnightReprint = Printing(
    oracleId = "32b401e9-163f-4917-a728-fc63b25ef602",
    name = "Benalish Knight",
    setCode = "10E",
    collectorNumber = "11",
    artist = "Zoltan Boros & Gabor Szikszai",
    imageUri = "https://cards.scryfall.io/normal/front/d/0/d0ae60d0-20d3-452c-9953-e229567c06f5.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
