package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plateau reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PlateauReprint = Printing(
    oracleId = "c7a15ca4-085f-4d92-8387-c3711c04c8fa",
    name = "Plateau",
    setCode = "LEB",
    collectorNumber = "280",
    artist = "Drew Tucker",
    imageUri = "https://cards.scryfall.io/normal/front/f/a/fad0bbc4-f760-47a2-aab6-0dbb66ee3a95.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
