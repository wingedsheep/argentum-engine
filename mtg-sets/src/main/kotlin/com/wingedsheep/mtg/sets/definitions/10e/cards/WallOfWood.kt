package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Wood reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfWoodReprint = Printing(
    oracleId = "aa5e6894-8c97-46f8-a0fd-07c6db51e9a7",
    name = "Wall of Wood",
    setCode = "10E",
    collectorNumber = "309",
    artist = "Rebecca Guay",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/1864e25e-f940-47e5-9439-ab721049c690.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
