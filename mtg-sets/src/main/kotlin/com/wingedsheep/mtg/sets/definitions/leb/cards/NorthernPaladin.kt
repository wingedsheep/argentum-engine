package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Northern Paladin reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val NorthernPaladinReprint = Printing(
    oracleId = "f5975294-508a-453e-893a-2fbea2487d17",
    name = "Northern Paladin",
    setCode = "LEB",
    collectorNumber = "30",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/4/b/4ba8493c-ae69-48d1-a050-a887ae27c83f.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
