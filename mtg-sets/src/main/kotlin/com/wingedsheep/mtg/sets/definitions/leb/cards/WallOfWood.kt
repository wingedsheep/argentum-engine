package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Wood reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfWoodReprint = Printing(
    oracleId = "aa5e6894-8c97-46f8-a0fd-07c6db51e9a7",
    name = "Wall of Wood",
    setCode = "LEB",
    collectorNumber = "226",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/1/a/1a5054a4-599d-49df-9a80-77eeed47891f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
