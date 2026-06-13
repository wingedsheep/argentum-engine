package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Swords reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfSwordsReprint = Printing(
    oracleId = "eb098958-50d3-4476-ba74-382033703ff9",
    name = "Wall of Swords",
    setCode = "LEB",
    collectorNumber = "43",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/b/e/be955e9a-e722-4cd7-8e3d-bab1889c255b.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.UNCOMMON,
)
