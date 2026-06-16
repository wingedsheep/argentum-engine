package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Fire reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfFireReprint = Printing(
    oracleId = "f38c8b47-e8e0-4d2f-b1da-d8d986805a48",
    name = "Wall of Fire",
    setCode = "LEB",
    collectorNumber = "182",
    artist = "Richard Thomas",
    imageUri = "https://cards.scryfall.io/normal/front/8/8/88baaea5-69ec-4756-86c2-9c9d73ca8ef1.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
