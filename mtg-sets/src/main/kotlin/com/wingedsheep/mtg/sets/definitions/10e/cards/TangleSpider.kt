package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tangle Spider reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * DST's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TangleSpiderReprint = Printing(
    oracleId = "597bd341-c5a0-47a3-9864-1fccab8a2a00",
    name = "Tangle Spider",
    setCode = "10E",
    collectorNumber = "303",
    artist = "Terese Nielsen",
    imageUri = "https://cards.scryfall.io/normal/front/8/f/8f1e75e1-a827-46c1-896f-cd8fbdf79fa8.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
