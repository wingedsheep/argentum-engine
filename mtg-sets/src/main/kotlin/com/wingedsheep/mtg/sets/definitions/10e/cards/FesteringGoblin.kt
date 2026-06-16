package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Festering Goblin reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ONS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FesteringGoblinReprint = Printing(
    oracleId = "66fb4764-d309-4c30-a2a4-474f9030dc87",
    name = "Festering Goblin",
    setCode = "10E",
    collectorNumber = "143",
    artist = "Thomas M. Baxa",
    imageUri = "https://cards.scryfall.io/normal/front/f/c/fce1a3a3-f0b8-4088-85f6-8cbdd1f29e65.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
