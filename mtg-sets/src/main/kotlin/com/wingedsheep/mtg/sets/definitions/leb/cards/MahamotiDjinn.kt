package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mahamoti Djinn reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MahamotiDjinnReprint = Printing(
    oracleId = "c39ea5f9-6ec0-4697-897b-779e326754a7",
    name = "Mahamoti Djinn",
    setCode = "LEB",
    collectorNumber = "65",
    artist = "Dan Frazier",
    imageUri = "https://cards.scryfall.io/normal/front/0/8/083f76c8-3e6d-4de5-b408-2f2394faed5c.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
