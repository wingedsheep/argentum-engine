package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Holy Strength reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HolyStrengthReprint = Printing(
    oracleId = "9357de36-f8be-4f49-b2c8-9fe9eaf82b07",
    name = "Holy Strength",
    setCode = "LEB",
    collectorNumber = "25",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/d/e/de989395-50bf-458a-a010-e12abe2e15a6.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
