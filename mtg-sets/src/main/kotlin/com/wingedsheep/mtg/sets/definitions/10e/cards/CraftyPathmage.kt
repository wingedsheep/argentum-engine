package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crafty Pathmage reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CraftyPathmageReprint = Printing(
    oracleId = "c4020178-71d9-4363-a0bd-b9a7eecd5d02",
    name = "Crafty Pathmage",
    setCode = "10E",
    collectorNumber = "77",
    artist = "Wayne England",
    imageUri = "https://cards.scryfall.io/normal/front/f/1/f186271c-2b03-4e0c-b922-27e23787d865.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
