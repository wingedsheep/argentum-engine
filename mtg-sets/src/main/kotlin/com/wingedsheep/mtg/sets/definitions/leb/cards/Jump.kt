package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Jump reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val JumpReprint = Printing(
    oracleId = "f7518456-45ed-41d4-bd3c-5aacea28eb35",
    name = "Jump",
    setCode = "LEB",
    collectorNumber = "61",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/e/5/e51e8a6e-1da8-4e6f-8433-9f0695926f04.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
