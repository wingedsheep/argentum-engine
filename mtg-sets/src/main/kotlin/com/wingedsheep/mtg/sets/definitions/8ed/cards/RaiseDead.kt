package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Raise Dead reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RaiseDeadReprint = Printing(
    oracleId = "cbc9c731-181a-4f00-a7b0-eb7e56eac2ea",
    name = "Raise Dead",
    setCode = "8ED",
    collectorNumber = "157",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cc29ec6b-555e-4472-8bd4-d7e898edabce.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
