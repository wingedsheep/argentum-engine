package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Treasure Hunter reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TreasureHunterReprint = Printing(
    oracleId = "71ae3bc0-28da-4e91-bffd-a49fe545b7a9",
    name = "Treasure Hunter",
    setCode = "10E",
    collectorNumber = "52",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/0/2/0282b59e-78ef-412d-bb76-fb337f32a213.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
