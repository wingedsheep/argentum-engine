package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Invisibility reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val InvisibilityReprint = Printing(
    oracleId = "de26b0c6-dfb7-45a8-9d7f-f8d45522d675",
    name = "Invisibility",
    setCode = "LEB",
    collectorNumber = "60",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/d/d/dde97b8f-7c10-48d3-8ae2-9f86158973ec.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
