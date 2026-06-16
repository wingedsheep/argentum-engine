package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Piker reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val GoblinPikerReprint = Printing(
    oracleId = "50608184-90d3-43d2-a221-deb186c78323",
    name = "Goblin Piker",
    setCode = "10E",
    collectorNumber = "209",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/c/e/ceae72d6-f2a2-4345-822d-6a7c2409a237.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
