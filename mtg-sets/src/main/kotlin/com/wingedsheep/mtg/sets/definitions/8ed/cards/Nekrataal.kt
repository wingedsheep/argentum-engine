package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Nekrataal reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NekrataalReprint = Printing(
    oracleId = "d40adf5d-5edf-48a0-98f3-fcdbf1b5f25c",
    name = "Nekrataal",
    setCode = "8ED",
    collectorNumber = "149",
    artist = "Adrian Smith",
    imageUri = "https://cards.scryfall.io/normal/front/c/5/c5c0d60f-0824-4498-81da-2c65980e3f98.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
