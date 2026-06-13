package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Bad Moon reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BadMoonReprint = Printing(
    oracleId = "fc5d3341-cbce-49e5-93cc-8add92479dca",
    name = "Bad Moon",
    setCode = "LEB",
    collectorNumber = "94",
    artist = "Jesper Myrfors",
    imageUri = "https://cards.scryfall.io/normal/front/b/f/bf812f48-633c-46ab-b0c3-4819ab1b4e49.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
