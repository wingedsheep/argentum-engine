package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Howl from Beyond reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HowlFromBeyondReprint = Printing(
    oracleId = "403cf6ae-48a9-4ea9-894e-7135cfca4e1b",
    name = "Howl from Beyond",
    setCode = "LEB",
    collectorNumber = "112",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/f/6/f6018459-d09b-489a-81be-933fd7d854c1.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
