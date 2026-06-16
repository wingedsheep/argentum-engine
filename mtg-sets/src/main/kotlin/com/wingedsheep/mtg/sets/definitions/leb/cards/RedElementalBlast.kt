package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Red Elemental Blast reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RedElementalBlastReprint = Printing(
    oracleId = "bb329a5c-b9f9-4973-a53f-090024146325",
    name = "Red Elemental Blast",
    setCode = "LEB",
    collectorNumber = "170",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/4/f/4fafd3f9-f7de-4d6e-8824-6b60866fc50f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
