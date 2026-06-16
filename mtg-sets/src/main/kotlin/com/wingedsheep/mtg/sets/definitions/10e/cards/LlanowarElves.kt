package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Llanowar Elves reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LlanowarElvesReprint = Printing(
    oracleId = "68954295-54e3-4303-a6bc-fc4547a4e3a3",
    name = "Llanowar Elves",
    setCode = "10E",
    collectorNumber = "274",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/c/7/c7a7fe6e-aa5a-4be6-a730-5cfff4fb89e3.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
