package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Unholy Strength reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val UnholyStrengthReprint = Printing(
    oracleId = "090d88a9-7f2d-4bd1-a30a-7c48d05068be",
    name = "Unholy Strength",
    setCode = "LEB",
    collectorNumber = "132",
    artist = "Douglas Shuler",
    imageUri = "https://cards.scryfall.io/normal/front/1/c/1c1c781d-1f27-40e3-9d79-0ebb6677e835.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
