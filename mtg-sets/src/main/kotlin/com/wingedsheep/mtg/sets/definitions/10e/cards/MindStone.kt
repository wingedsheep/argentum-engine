package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Stone reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * WTH's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MindStoneReprint = Printing(
    oracleId = "c97361b5-af16-4a7b-af85-a429dbaf4ad2",
    name = "Mind Stone",
    setCode = "10E",
    collectorNumber = "335",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/5/2/52284689-f2e0-442d-80d0-9e766759e2bc.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
