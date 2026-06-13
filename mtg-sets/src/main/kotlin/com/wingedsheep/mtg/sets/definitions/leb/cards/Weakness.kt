package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Weakness reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WeaknessReprint = Printing(
    oracleId = "f07a24c0-bf3c-4733-9473-c6be3b16950e",
    name = "Weakness",
    setCode = "LEB",
    collectorNumber = "135",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/1/6/16137fa6-1b5c-49e7-ad79-dda4b7019a59.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
