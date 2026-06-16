package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nekrataal reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NekrataalReprint = Printing(
    oracleId = "d40adf5d-5edf-48a0-98f3-fcdbf1b5f25c",
    name = "Nekrataal",
    setCode = "10E",
    collectorNumber = "163",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/f/c/fca42df8-a9a2-4391-8dec-7097f788c250.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
