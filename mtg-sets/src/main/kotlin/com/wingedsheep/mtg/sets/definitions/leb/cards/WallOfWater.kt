package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Water reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfWaterReprint = Printing(
    oracleId = "608cc65c-f99a-4ca3-be24-190d2556b411",
    name = "Wall of Water",
    setCode = "LEB",
    collectorNumber = "91",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/3/4/34887689-0adb-4ead-87a5-1d8fd77b6278.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
