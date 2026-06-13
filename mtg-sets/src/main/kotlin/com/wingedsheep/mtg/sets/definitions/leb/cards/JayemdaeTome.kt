package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Jayemdae Tome reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JayemdaeTomeReprint = Printing(
    oracleId = "39ee576a-0803-4063-9c84-f2b537e4d44c",
    name = "Jayemdae Tome",
    setCode = "LEB",
    collectorNumber = "255",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/e/4/e48b1c51-c0fd-4c08-8631-80f507b04d28.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.RARE,
)
