package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Essence Drain reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DST's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EssenceDrainReprint = Printing(
    oracleId = "4bf5402f-7bba-4c0c-aebd-1b231768f2b2",
    name = "Essence Drain",
    setCode = "10E",
    collectorNumber = "141",
    artist = "Jim Nelson",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8af61c18-a7ac-42c8-a942-d2f546c1ef57.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
