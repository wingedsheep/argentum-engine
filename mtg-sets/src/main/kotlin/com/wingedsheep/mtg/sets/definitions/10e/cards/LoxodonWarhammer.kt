package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Loxodon Warhammer reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LoxodonWarhammerReprint = Printing(
    oracleId = "dba35ac5-7ad3-488a-a006-6b9a1d54eea5",
    name = "Loxodon Warhammer",
    setCode = "10E",
    collectorNumber = "332",
    artist = "Jeremy Jarvis",
    imageUri = "https://cards.scryfall.io/normal/front/f/5/f5f3f215-389b-4d86-8393-65e5af4ab981.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
