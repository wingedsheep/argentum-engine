package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Prodigal Pyromancer reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PLC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ProdigalPyromancerReprint = Printing(
    oracleId = "29b3cb3e-aeb1-43eb-8095-9f7881aa34bd",
    name = "Prodigal Pyromancer",
    setCode = "10E",
    collectorNumber = "221",
    artist = "Jeremy Jarvis",
    imageUri = "https://cards.scryfall.io/normal/front/e/6/e6023c95-8cd8-4f12-960a-6557fbefd628.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
