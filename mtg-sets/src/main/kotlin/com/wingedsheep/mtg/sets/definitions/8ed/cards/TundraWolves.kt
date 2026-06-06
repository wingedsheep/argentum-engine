package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Tundra Wolves reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val TundraWolvesReprint = Printing(
    oracleId = "2155864e-5788-4e72-a800-e2cf25cf59a7",
    name = "Tundra Wolves",
    setCode = "8ED",
    collectorNumber = "54",
    artist = "Richard Sardinha",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/030d414a-5155-41cc-8b01-07d297254d1c.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
