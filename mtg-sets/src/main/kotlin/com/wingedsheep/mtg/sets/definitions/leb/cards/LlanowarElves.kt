package com.wingedsheep.mtg.sets.definitions.leb.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Llanowar Elves reprint in LEB.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the LEB-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LlanowarElvesReprint = Printing(
    oracleId = "68954295-54e3-4303-a6bc-fc4547a4e3a3",
    name = "Llanowar Elves",
    setCode = "LEB",
    collectorNumber = "211",
    artist = "Anson Maddocks",
    imageUri = "https://cards.scryfall.io/normal/front/a/b/abd80204-e9ba-483f-9b75-a69712545ba9.jpg",
    releaseDate = "1993-10-04",
    rarity = Rarity.COMMON,
)
