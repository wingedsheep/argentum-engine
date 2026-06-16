package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tundra Wolves reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TundraWolvesReprint = Printing(
    oracleId = "2155864e-5788-4e72-a800-e2cf25cf59a7",
    name = "Tundra Wolves",
    setCode = "10E",
    collectorNumber = "54",
    artist = "Richard Sardinha",
    imageUri = "https://cards.scryfall.io/normal/front/1/5/15f573db-f4f8-4311-ba47-234e5171da3d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
