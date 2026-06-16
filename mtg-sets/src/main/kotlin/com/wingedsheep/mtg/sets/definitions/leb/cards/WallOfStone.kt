package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Stone reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfStoneReprint = Printing(
    oracleId = "cd4cadb4-3156-49bd-b36e-12ba5c85938b",
    name = "Wall of Stone",
    setCode = "LEB",
    collectorNumber = "183",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/3/2/329ba196-a107-41ac-b02a-5f8b10ecd130.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
