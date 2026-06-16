package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Will-o'-the-Wisp reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WillOTheWispReprint = Printing(
    oracleId = "8b60fcfe-fb90-4a00-a708-25b59bfc9b5a",
    name = "Will-o'-the-Wisp",
    setCode = "LEB",
    collectorNumber = "136",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/4/b/4b60630c-f97c-43be-8410-53a68613b735.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
