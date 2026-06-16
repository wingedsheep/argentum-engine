package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Ice reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfIceReprint = Printing(
    oracleId = "1945e165-7636-4727-89ba-3811251ef175",
    name = "Wall of Ice",
    setCode = "LEB",
    collectorNumber = "225",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/c/c/cc05a648-7719-4ed3-aa3b-648463ee2869.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
