package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Air reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfAirReprint = Printing(
    oracleId = "b2d3da40-e2f7-4480-9da9-33019d6f4071",
    name = "Wall of Air",
    setCode = "LEB",
    collectorNumber = "90",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/71904b59-55dd-4074-9d50-c5bb0fb7266f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
